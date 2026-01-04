# Design

## Overview

Introduce an auto-recording flow that polls live status before recording begins and monitors live status during recording. When the live ends, stop recording and remux to MP4. Use existing services where possible and add minimal new configuration.

## Architecture Changes

- RecordingManager: add a WAITING state and a polling loop before starting recording.
- RecordingManager: while recording, periodically verify live is still active; if not, stop the task.
- TaskStatus: add WAITING and FINALIZING (optional) states.
- Configuration: add polling interval and max wait settings.

## Workflow

1. /start receives request.
2. RecordingManager creates task with status WAITING when auto mode is enabled.
3. Poll loop checks LiveStreamDetector.checkLiveStatus(douyinId).
4. When live starts, proceed to stream extraction and start recording.
5. During recording, monitor live status every poll interval. If live ends, stop recording and remux.
6. On stop, finalize (remux). If remux fails, keep temp .flv and report error.

## Configuration

Add the following to application.properties:

- recording.auto-enabled (boolean, default true; overridden by /start `auto` param)
- recording.poll-interval-ms (default 120000)
- recording.max-wait-ms (default 3600000)
- recording.end-detect-grace-ms (default 15000) to avoid flapping

## Data Model Updates

RecordingTask:
- add tempOutputPath (already added)
- add autoEnabled (optional) if needed for per-task settings

TaskStatus:
- add WAITING for pre-live polling
- add FINALIZING (optional) for remux phase

## Error Handling

- If live status API fails transiently, retry on next poll.
- If max wait exceeded, mark FAILED with message "live not started in time".
- If stop is requested while WAITING, mark CANCELLED and stop polling.

## Concurrency

- Each auto task uses a lightweight polling loop on the executor.
- Respect existing max concurrent tasks for active recordings. WAITING tasks do not count toward the limit.

## Logging

- Debug logs for each poll attempt.
- Info logs for state transitions and start/stop actions.

## Testing Plan

- Unit tests for polling logic (WAITING -> RECORDING).
- Unit tests for stop during WAITING.
- Integration test simulating live transitions and ensuring auto stop.
