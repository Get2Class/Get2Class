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

// Interface PUT /karma
describe("Unmocked: PUT /karma", () => {
    // Input: valid request body contains subject id and karma
    // Expected status code: 200
    // Expected behavior: should return success status code and body contains karma update acknowledgement and message
    // Expected output: acknowledged, message
    test("Update valid user karma points", async () => {
        const res = await request(server).put("/karma").send({
            sub: "123",
            karma: 60
        });

        expect(res.statusCode).toBe(200);
        expect(res.body).toHaveProperty('acknowledged');
        expect(res.body).toHaveProperty('message');
    });

    // Input: valid request body contains subject id and karma
    // Expected status code: 400
    // Expected behavior: should return error status code and body is empty with text error message
    // Expected output: empty body and text error message "User not found"
    test("Unable to update a nonexistent user karma points", async () => {
        const res = await request(server).put("/karma").send({
            sub: "321",
            karma: 60
        });

        expect(res.statusCode).toBe(400);
        expect(res.text).toBe("User not found");
        expect(res.body).toEqual({});
    });

    // Input: missing data field in request body
    // Expected status code: 400
    // Expected behavior: should return error status code and body contains 'errors' due to missing fields in request body
    // Expected output: errors
    test("Missing data field when updating karma points", async () => {
        const res = await request(server).put("/karma").send({
            sub: "123"
        });

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });

    // Input: valid request body contains subject id and karma
    // Expected status code: 400
    // Expected behavior: should return error status code and body is empty with text error message
    // Expected output: empty body and text error message "Unable to update karma"
    test("Updating karma with 0 points", async () => {
        const res = await request(server).put("/karma").send({
            sub: "123",
            karma: 0
        });

        expect(res.statusCode).toBe(400);
        expect(res.text).toBe("Unable to update karma");
        expect(res.body).toEqual({});
    });

    // Input: empty request body
    // Expected status code: 400
    // Expected behavior: should return error status code and body contains 'errors' due to missing request body
    // Expected output: errors
    test("No body in request", async () => {
        const res = await request(server).put("/karma").send({});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });
});