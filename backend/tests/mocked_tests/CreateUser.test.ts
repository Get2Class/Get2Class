import { Db } from 'mongodb';
import { serverReady, cronResetAttendance } from '../../index';
import { client } from '../../services';
import request from 'supertest';

let server: any;

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
    await cronResetAttendance.stop();
    await server.close();
});

// Interface POST /user
describe("Mocked: POST /user", () => {
    // Mocked behavior: client db/collection throws an error
    // Input: valid email, subject id, and name
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach get2class database", async () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const res = await request(server).post('/user').send({
            email: "createnewuser@gmail.com",
            sub: "123",
            name: "New User"
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error at the first client.db.collection call
    // Input: valid email, subject id, and name
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to create user in users collection", async () => {
        const userCollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce({
            collection: userCollectionMock
        } as any);

        const res = await request(server).post('/user').send({
            email: "createnewuser@gmail.com",
            sub: "123",
            name: "New User"
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(userCollectionMock).toHaveBeenCalledWith('users');
        expect(userCollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        userCollectionMock.mockRestore();
        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error throws an error at the second client.db.collection call
    // Input: valid email, subject id, and name
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to create schedule in schedules collection", async () => {
        const mockUserInsertResult = { sub: "123" };
        const insertUserMock = jest.fn().mockResolvedValueOnce(mockUserInsertResult);

        const userCollectionMock = jest.fn().mockImplementationOnce(() => {
            return { insertOne: insertUserMock }
        });

        const scheduleCollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce({
            collection: userCollectionMock
        } as any).mockReturnValueOnce({
            collection: scheduleCollectionMock
        } as any);

        const res = await request(server).post('/user').send({
            email: "createnewuser@gmail.com",
            sub: "123",
            name: "New User"
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(userCollectionMock).toHaveBeenCalledWith('users');
        expect(userCollectionMock).toHaveBeenCalledTimes(1);
        expect(scheduleCollectionMock).toHaveBeenCalledWith('schedules');
        expect(scheduleCollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(2);

        userCollectionMock.mockRestore();
        scheduleCollectionMock.mockRestore();
        dbSpy.mockRestore();
    });
});