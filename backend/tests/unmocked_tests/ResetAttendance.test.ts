import { Server } from 'http';
import { serverReady, cronResetAttendance, resetAttendanceController } from '../../index';
import { client } from '../../services';
import request from 'supertest';

let server: Server;

beforeAll(async () => {
    server = await serverReady;

    await client.db("get2class").collection("schedules").insertOne({ 
        email: "asdfasdf@gmail.com",
        sub: "123",
        name: "asdfasdf",
        fallCourseList: [
            {
                name: "CPEN 321 - Software Engineering",
                daysBool: "[true, false, true, false, false]",
                startTime: "(15, 30)",
                endTime: "(17, 0)",
                startDate: "2025-01-06",
                endDate: "2025-04-07",
                location: "CHBE - Room 102",
                credits: 4,
                format: "Lecture",
                attended: true
            }
        ],
        winterCourseList: [
            {
                name: "CPEN 321 - Software Engineering",
                daysBool: "[true, false, true, false, false]",
                startTime: "(15, 30)",
                endTime: "(17, 0)",
                startDate: "2025-01-06",
                endDate: "2025-04-07",
                location: "CHBE - Room 102",
                credits: 4,
                format: "Lecture",
                attended: true
            }
        ],
        summerCourseList: [
            {
                name: "CPEN 321 - Software Engineering",
                daysBool: "[true, false, true, false, false]",
                startTime: "(15, 30)",
                endTime: "(17, 0)",
                startDate: "2025-01-06",
                endDate: "2025-04-07",
                location: "CHBE - Room 102",
                credits: 4,
                format: "Lecture",
                attended: true
            }
        ]
     });
});

afterAll(async () => {
    await client.db("get2class").collection("schedules").deleteOne({
        sub: "123"
    });
    await client.close();
    cronResetAttendance.stop();
    await new Promise((resolve) => { resolve(server.close()); });
});

// Unmocked Tests for ResetAttendance
describe("Unmocked: Test reset attendance logic", () => {
    // Input: none
    // Expected status code: 200
    // Expected behavior: All of the attended fields of the courses within a user's courseList should be set back to false
    // Expected output: attended = false
    test("Full run through of reset attendance controller for all courseLists", async () => {
        await resetAttendanceController.resetAttendance();

        const res1 = await request(server).get("/schedule").query({
            sub: "123",
            term: "winterCourseList"
        });

        const res2 = await request(server).get("/schedule").query({
            sub: "123",
            term: "fallCourseList"
        });

        const res3 = await request(server).get("/schedule").query({
            sub: "123",
            term: "summerCourseList"
        });

        expect(res1.statusCode).toBe(200);
        expect(res1.body).toHaveProperty('courseList');
        expect(res1.body.courseList[0]).toHaveProperty('attended', false);

        expect(res2.statusCode).toBe(200);
        expect(res2.body).toHaveProperty('courseList');
        expect(res2.body.courseList[0]).toHaveProperty('attended', false);

        expect(res3.statusCode).toBe(200);
        expect(res3.body).toHaveProperty('courseList');
        expect(res3.body.courseList[0]).toHaveProperty('attended', false);
    });
});