const { serverReady, cronResetAttendance } = require("../../index");
const { mySchedule, myUser, myDBScheduleItem, Init } = require("../utils");
import { client } from '../../services';
import request from 'supertest';
import { Server } from "http";

let server: Server;

beforeAll(async () => {
    // Wait for the server to be ready
    server = await serverReady;  
    let dbScheduleItem = myDBScheduleItem;
    dbScheduleItem.fallCourseList = mySchedule.courses;
    await client.db("get2class").collection("schedules").insertOne(myDBScheduleItem);
});

afterAll(async () => {
    // Initialize DB for tests
    await client.db("get2class").collection("schedules").deleteOne({
        sub: myUser.sub
    });
    await client.close();
    cronResetAttendance.stop();
    await server.close();
});

// Interface DELETE /schedule
describe("Mocked: DELETE /schedule", () => {
    test("Unable to reach get2class database", async () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const req = {sub: myUser.sub, fallCourseList: "fallCourseList"};
        const res = await request(server).delete("/schedule").send(req);
        
        expect(res.statusCode).toStrictEqual(500);
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);

        dbSpy.mockRestore();
    });
});