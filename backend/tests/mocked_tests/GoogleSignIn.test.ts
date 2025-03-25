import { OAuth2Client } from "google-auth-library";
import { serverReady, cronResetAttendance } from '../../index';
import { client } from '../../services';
import request from 'supertest';
import { Server } from "http";

let server: Server;

jest.mock('google-auth-library', () => {
    return {
        OAuth2Client: jest.fn().mockImplementation()
    };
});

beforeAll(async () => {
    server = await serverReady;
});

afterAll(async () => {
    await client.close();
    cronResetAttendance.stop();
    await new Promise((resolve) => { resolve(server.close()); });
    jest.resetAllMocks();
});

// Interface POST /tokensignin
describe("Unmocked: POST /tokensignin", () => {
    // Input: valid idToken and audience key
    // Expected status code: 200
    // Expected behavior: success status code and should return back to the user the subject id
    // Expected output: sub
    test("Return a sub from payload", async () => {
        const mockVerifyIdToken = jest.fn().mockResolvedValue({
            getPayload: () => ({ sub: "123" })
        });

        (OAuth2Client as unknown as jest.Mock).mockImplementation(() => ({
            verifyIdToken: mockVerifyIdToken
        }));

        const res = await request(server).post('/tokensignin').send({
            idToken: "mock-id-token",
            audience: "mock-audience"
        });

        expect(mockVerifyIdToken).toHaveBeenCalledWith({
            idToken: "mock-id-token",
            audience: "mock-audience"
        });
        expect(mockVerifyIdToken).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(200);
        expect(res.body).toEqual({ sub: "123" });
    });

    // Input: empty body
    // Expected status code: 400
    // Expected behavior: error status code returned and 'errors' is returned due to missing fields
    // Expected output: errors
    test("No data sent in body", async () => {
        const mockVerifyIdToken = jest.fn().mockResolvedValue({
            getPayload: () => ({ sub: "123" })
        });

        (OAuth2Client as unknown as jest.Mock).mockImplementation(() => ({
            verifyIdToken: mockVerifyIdToken
        }));

        const res = await request(server).post('/tokensignin').send({});

        expect(mockVerifyIdToken).toHaveBeenCalledTimes(0);
        expect(res.statusCode).toBe(400);
        expect(res.body).toHaveProperty('errors');
    });
});