import { Server } from 'http';
import { serverReady, cronResetAttendance } from '../../index';
import { client } from '../../services';
import request from 'supertest';

let server: Server;

beforeAll(async () => {
    server = await serverReady;

    await client.db("get2class").collection("users").insertOne({ 
        email: "asdfasdf@gmail.com",
        sub: "123",
        name: "asdfasdf",
        karma: 0,
        notificationTime: 15,
        notificationsEnabled: true
     });
});

afterAll(async () => {
    await client.db("get2class").collection("users").deleteOne({
        sub: "123"
    });
    await client.close();
    cronResetAttendance.stop();
    await new Promise((resolve) => { resolve(server.close()); });
});

// Interface GET /user
describe("Unmocked: GET /user", () => {
    // Input: valid subject id
    // Expected status code: 200
    // Expected behavior: should return all of the information of the found user
    // Expected output: email, sub, name, karma, notificationTime, notificationsEnabled
    test("Found valid user", async () => {
        const res = await request(server).get("/user").query({sub: "123"});

        expect(res.statusCode).toBe(200);
        expect(res.body).toHaveProperty('email');
        expect(res.body).toHaveProperty('sub');
        expect(res.body).toHaveProperty('name');
        expect(res.body).toHaveProperty('karma');
        expect(res.body).toHaveProperty('notificationTime');
        expect(res.body).toHaveProperty('notificationsEnabled');
    });

    // Input: valid subject id
    // Expected status code: 400
    // Expected behavior: should return error status code and contain a message text mentioning that the user does not exist
    // Expected output: empty body and "User does not exist" text
    test("No valid user", async () => {
        const res = await request(server).get("/user").query({sub: "blahblah"});

        expect(res.statusCode).toBe(400);
        expect(res.text).toBe("User does not exist");
        expect(res.body).toEqual({});
    });

    // Input: a null type of object for subject id
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body for missing field
    // Expected output: errors
    test("Incorrect type in query", async () => {
        const res = await request(server).get("/user").query({sub: null});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });

    // Input: empty query
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body for missing fields
    // Expected output: errors
    test("Empty query", async () => {
        const res = await request(server).get("/user").query({});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });
});
