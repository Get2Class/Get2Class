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

// Interface GET /notification_settings
describe("Unmocked: GET /notification_settings", () => {
    // Input: valid subject id
    // Expected status code: 200
    // Expected behavior: should return success status code and body contains user notification properties
    // Expected output: notificationsEnabled, notificationTime
    test("Get valid user notification settings", async () => {
        const res = await request(server).get("/notification_settings").query({sub: "123"});

        expect(res.statusCode).toBe(200);
        expect(res.body).toHaveProperty('notificationsEnabled');
        expect(res.body).toHaveProperty('notificationTime');
    });

    // Input: valid subject id
    // Expected status code: 400
    // Expected behavior: should return error status code, empty body, and error text message
    // Expected output: empty body and "User not found" text message
    test("Unable to get nonexistent user notification settings", async () => {
        const res = await request(server).get("/notification_settings").query({sub: "blahblah"});

        expect(res.statusCode).toBe(400);
        expect(res.text).toBe("User not found");
        expect(res.body).toEqual({});
    });

    // Input: null type subject id
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body due to invalid field type
    // Expected output: errors
    test("Invalid type in query", async () => {
        const res = await request(server).get("/notification_settings").query({sub: null});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });

    // Input: empty query
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body due to missing fields
    // Expected output: errors
    test("No sub in query", async () => {
        const res = await request(server).get("/notification_settings").query({});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });
});

// Interface PUT /notification_settings
describe("Unmocked: PUT /notification_settings", () => {
    // Input: valid request body containing subject id, notificationTime, and notificationsEnabled
    // Expected status code: 200
    // Expected behavior: should return a success status code along with a body containing the acknowledged update request and message
    // Expected output: acknowledged, message
    test("Update valid user notification settings", async () => {
        const res = await request(server).put("/notification_settings").send({
            sub: "123",
            notificationTime: 10,
            notificationsEnabled: false
        });

        expect(res.statusCode).toBe(200);
        expect(res.body).toHaveProperty('acknowledged');
        expect(res.body).toHaveProperty('message');
    });

    // Input: valid request body containing subject id, notificationTIme, and notificationsEnabled
    // Expected status code: 400
    // Expected behavior: should return error status code due to subject id not being found in the database, body is empty and error text message is returned
    // Expected output: empty body and error text message "Unable to modify data"
    test("Unable to update nonexistent user notification settings", async () => {
        const res = await request(server).put("/notification_settings").send({
            sub: "321",
            notificationTime: 10,
            notificationsEnabled: false
        });

        expect(res.statusCode).toBe(400);
        expect(res.text).toBe("Unable to modify data");
        expect(res.body).toEqual({});
    });

    // Input: missing fields in request body
    // Expected status code: 400
    // Expected behavior: should return error status code due to missing fields in request body such as: notificationTime, notificationsEnabled, should contain 'errors' in the body
    // Expected output: errors
    test("Missing data in body of request when updating user notification settings", async () => {
        const res = await request(server).put("/notification_settings").send({
            sub: "123",
        });

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });

    // Input: empty request body
    // Expected status code: 400
    // Expected behavior: should return error status code due to empty request body, should contain 'errors' in body
    // Expected output: errors
    test("Missing req.body in request", async () => {
        const res = await request(server).put("/notification_settings").send({});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });
});