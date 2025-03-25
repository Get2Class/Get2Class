import { Server } from 'http';
import { serverReady, cronResetAttendance } from '../../index';
import { client } from '../../services';
import request from 'supertest';
import { Db } from 'mongodb';

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
describe("Mocked: GET /user", () => {
    // Mocked behavior: client db/collection throws an error
    // Input: valid subject id
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach get2class database when getting user", async () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const res = await request(server).get('/user').query({ sub: "123" });

        expect(res.statusCode).toStrictEqual(500);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error
    // Input: valid subject id
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach users collection", async () => {
        const userCollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbMock = {
            collection: userCollectionMock
        } as Partial<jest.Mocked<Db>>;

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce(
            dbMock as Db
        );

        const res = await request(server).get('/user').query({ sub: "123" });

        expect(res.statusCode).toStrictEqual(500);
        expect(userCollectionMock).toHaveBeenCalledWith('users');
        expect(userCollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);
    });
});