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

// Interface PUT /karma
describe("Mocked: PUT /karma", () => {
    // Mocked behavior: client db/collection throws an error
    // Input: valid subject id and karma
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach get2class database when finding user to update karma for", async () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const res = await request(server).put('/karma').send({ 
            sub: "123", 
            karma: 60
        });
    
        expect(res.statusCode).toStrictEqual(500);            
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);
    
        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error at the first client.db.collection call
    // Input: valid subject id and karma
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach first get2class db user collection call", async () => {
        const userCollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbMock = {
            collection: userCollectionMock
        } as Partial<jest.Mocked<Db>>

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce(
            dbMock as Db
        );

        const res = await request(server).put('/karma').send({ 
            sub: "123", 
            karma: 60
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(userCollectionMock).toHaveBeenCalledWith('users');
        expect(userCollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        userCollectionMock.mockRestore();
        dbSpy.mockRestore();
    });

    // Mocked behavior: client db/collection throws an error at the second client.db.collection call
    // Input: valid subject id and karma
    // Expected status code: 500
    // Expected behavior: should return error response due to db/collection failiure
    // Expected output: error response with status 500 and error message "Database connection error"
    test("Unable to reach second get2class db user collection call", async () => {
        const mockFindOne = jest.fn().mockResolvedValueOnce({
            findOne: () => ({ sub: "123" })
        });

        const user1CollectionMock = jest.fn().mockImplementationOnce(() => {
            return { findOne: mockFindOne }
        });

        const dbMock1 = {
            collection: user1CollectionMock
        } as Partial<jest.Mocked<Db>>;

        const user2CollectionMock = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbMock2 = {
            collection: user2CollectionMock
        } as Partial<jest.Mocked<Db>>

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce(
            dbMock1 as Db
        ).mockReturnValueOnce(
            dbMock2 as Db
        );

        const res = await request(server).put('/karma').send({
            sub: "123",
            karma: 60
        });

        expect(res.statusCode).toStrictEqual(500);
        expect(user1CollectionMock).toHaveBeenCalledWith('users');
        expect(user1CollectionMock).toHaveBeenCalledTimes(1);
        expect(user2CollectionMock).toHaveBeenCalledWith('users');
        expect(user2CollectionMock).toHaveBeenCalledTimes(1);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(2);

        user1CollectionMock.mockRestore();
        user2CollectionMock.mockRestore();
        dbSpy.mockRestore();
    });
});