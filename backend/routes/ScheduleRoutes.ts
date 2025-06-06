import { query, body } from 'express-validator';
import { ScheduleController } from '../controllers/ScheduleController';

const controller = new ScheduleController();

export const ScheduleRoutes = [
    {
        method: "get",
        route: "/schedule",
        action: controller.getSchedule.bind(controller),
        validation: [
            query("sub").exists().isString(),
            query("term").exists().isString()
        ]
    },
    {
        method: "put",
        route: "/schedule",
        action: controller.saveSchedule.bind(controller),
        validation: [
            body("sub").exists().isString()
        ]
    },
    {
        method: "delete",
        route: "/schedule",
        action: controller.clearSchedule.bind(controller),
        validation: [
            body("sub").exists().isString()
        ]
    },
    {
        method: "get",
        route: "/attendance",
        action: controller.getAttendance.bind(controller),
        validation: [
            query("sub").exists().isString(),
            query("className").exists().isString(),
            query("classFormat").exists().isString(),
            query("term").exists().isString()
        ]
    },
    {
        method: "put",
        route: "/attendance",
        action: controller.updateAttendance.bind(controller),
        validation: [
            body("sub").exists().isString(),
            body("className").exists().isString(),
            body("classFormat").exists().isString(),
            body("term").exists().isString()
        ]
    }
];