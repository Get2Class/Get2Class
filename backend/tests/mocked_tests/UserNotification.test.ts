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

// Interface GET /notification_settings
describe("Mocked: GET /notification_settings", () => {
    // Mocked behavior: client db/collection throws an error
    // Input: valid subject id
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach get2class db when getting user notification settings", async () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const res = await request(server).get("/notification_settings").query({ sub: "123" });

        expect(res.statusCode).toStrictEqual(500);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error at the first client.db.collection call
    // Input: valid subject id
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach get2class db users collection", async () => {
        const userCollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbMock = {
            collection: userCollectionMock
        } as Partial<jest.Mocked<Db>>;

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce(
            dbMock as Db
        );

        const res = await request(server).get("/notification_settings").query({ sub: "123" });

        expect(res.statusCode).toStrictEqual(500);
        expect(userCollectionMock).toHaveBeenCalledWith('users');
        expect(userCollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        userCollectionMock.mockRestore();
        dbSpy.mockRestore();
    });
});

// Interface PUT /notification_settings
describe("Mocked: PUT /notification_settings", () => {
    // Mocked behavior: client db/collection throws an error
    // Input: valid subject id, notificationTime, and notificationsEnabled fields
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach get2class db when updating user notification settings", async () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const res = await request(server).put("/notification_settings").send({
            sub: "123",
            notificationTime: 10,
            notificationsEnabled: false
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error at the first client.db.collection call
    // Input: valid subject id, notificationTime, and notificationsEnabled fields
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Database error when attempting to reach get2class db users collection", async () => {
        const userCollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbMock = {
            collection: userCollectionMock
        } as Partial<jest.Mocked<Db>>;

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce(
            dbMock as Db
        );

        const res = await request(server).put("/notification_settings").send({
            sub: "123",
            notificationTime: 10,
            notificationsEnabled: false
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(userCollectionMock).toHaveBeenCalledWith('users');
        expect(userCollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        userCollectionMock.mockRestore();
        dbSpy.mockRestore();
    });
});