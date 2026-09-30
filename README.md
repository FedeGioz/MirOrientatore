# MirOrientatore

Teacher app for the orientation days at ITIS Mario Delpozzo (Cuneo). It runs on the teacher's tablet, drives the school's MiR robot through a guided tour and coordinates the visitors' phones, which use the student app [MirOriento](https://github.com/FedeGioz/MirOriento).

It reuses the robot control code from [MiRage](https://github.com/FedeGioz/MiRage) and adds a server for the students on top of it.

## Features

- Robot control from MiRage: login, live status, joystick, maps (with the full map image), missions and sounds
- Tour page to run the guided tour as a sequence of robot missions
- Quiz library: pick a quiz and send it to every connected student, answers come back in real time
- List of connected students, with the option to hand the joystick to one of them
- Embedded WebSocket server that the student app connects to

## How it works

```
Student phones (MirOriento)  <--WebSocket-->  Teacher tablet (MirOrientatore)  <--REST / rosbridge-->  MiR robot
```

The tablet runs a Ktor server (Netty engine) on port 8080, with WebSocket pings every 5 seconds to spot phones that dropped off. The app keeps the list of connected students, pushes quizzes, robot status and map updates, and receives answers and joystick commands. A joystick command from a student is forwarded to the robot's rosbridge interface only if that student currently has control, so the teacher can take it back at any time.

Robot communication works as in MiRage: REST API for status, maps, missions and sounds, rosbridge WebSocket for driving.

## Tech stack

- Kotlin Multiplatform with Compose Multiplatform (Android target)
- Ktor server with WebSockets, Ktor client, kotlinx.serialization

## Running it

1. Open the project in Android Studio and run the `composeApp` configuration on the tablet
2. Connect the tablet to the robot and turn on its hotspot for the students
3. Log in with a robot user account and start the tour

The interface is in Italian, since it was made for an Italian school.
