import { Server } from 'http';
import { serverReady, cronResetAttendance } from '../../index';
import { client } from '../../services';
import { Db } from 'mongodb';

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

// Mocked Tests for ResetAttendance
describe("Mocked: Test reset attendance logic", () => {
    // Input: none
    // Expected status code: none
    // Expected behavior: should throw a database connection error
    // Expected output: none
    test("Throw error on first database call", () => {
        const dbSpy = jest.spyOn(client, "db").mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        cronResetAttendance.now();

        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(1);
    });

    // Input: none
    // Expected status code: none
    // Expected behavior: should throw a database connection error
    // Expected output: none
    test("Throw error on second database call", () => {
        const mockToArrayResult = [
            { 
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
            }
        ];
        const toArrayMock = jest.fn().mockResolvedValueOnce(mockToArrayResult);

        const findMock = jest.fn().mockImplementationOnce(() => {
            return { toArray: toArrayMock }
        });

        const scheduleCollectionMock1 = jest.fn().mockImplementationOnce(() => {
            return { find: findMock }
        });

        const dbMock1 = {
            collection: scheduleCollectionMock1
        } as Partial<jest.Mocked<Db>>;

        const scheduleCollectionMock2 = jest.fn().mockImplementationOnce(() => {
            throw new Error("Database connection error");
        });

        const dbMock2 = {
            collection: scheduleCollectionMock2
        } as Partial<jest.Mocked<Db>>;

        const dbSpy = jest.spyOn(client, "db").mockReturnValueOnce(
            dbMock1 as Db
        ).mockReturnValueOnce(
            dbMock2 as Db
        );

        cronResetAttendance.now();
        
        expect(dbSpy).toHaveBeenCalledWith('get2class');
        expect(dbSpy).toHaveBeenCalledTimes(2);
    });
});