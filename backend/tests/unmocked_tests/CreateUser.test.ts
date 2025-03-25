import { Server } from 'http';
import { serverReady, cronResetAttendance } from '../../index';
import { client } from '../../services';
import request from 'supertest';

let server: Server;

beforeAll(async () => {
    server = await serverReady;
});

afterAll(async () => {
    await client.db("get2class").collection("users").deleteOne({
        sub: "123"
    });
    await client.db("get2class").collection("schedules").deleteOne({
        sub: "123"
    });
    await client.close();
    cronResetAttendance.stop();
    await new Promise((resolve) => { resolve(server.close()); });
});

// Interface POST /user
describe("Unmocked: POST /user", () => {
    // Input: valid email, subject id, and name
    // Expected status code: 200
    // Expected behavior: should return status success and a body acknowledging user and schedule db updates
    // Expected output: userAcknowledged, scheduleAcknowledged, message
    test("Create a new user", async () => {
        const res = await request(server).post("/user").send({
            email: "createnewuser@gmail.com",
            sub: "123",
            name: "New User"
        });

        expect(res.statusCode).toBe(200);
        expect(res.body).toHaveProperty('userAcknowledged');
        expect(res.body).toHaveProperty('scheduleAcknowledged');
        expect(res.body).toHaveProperty('message');
    });

    // Input: valid subject id and name
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body for missing field
    // Expected output: errors
    test("Missing data when creating user", async () => {
        const res = await request(server).post("/user").send({
            sub: "123",
            name: "New User"
        });

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });

    // Input: invalid email type, valid subject id, valid name
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body for field type
    // Expected output: errors
    test("Incorrect type in data when creating user", async () => {
        const res = await request(server).post("/user").send({
            email: null,
            sub: "123",
            name: "New User"
        });

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });

    // Input: empty body
    // Expected status code: 400
    // Expected behavior: should return error status code and contain 'errors' in body for missing request body
    // Expected output: errors
    test("Missing req.body when creating user", async () => {
        const res = await request(server).post("/user").send({});

        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });
})