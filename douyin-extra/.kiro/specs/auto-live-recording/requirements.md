# Requirements

## Summary

Add an auto-record mode that waits for a streamer to go live, starts recording when live, and stops recording when the live ends. After stop, remux to MP4 to ensure playable output.

## Scope

- Applies to the existing /api/recordings/start workflow.
- Adds an optional `auto` parameter to control auto-record mode.
- When auto mode is enabled, the task will poll the live status until live starts.
- When live ends, the task stops recording and finalizes to MP4.

## Definitions

- Live Poll: periodic check of live status using the existing live status API.
- Auto Recording: wait-for-live plus auto-stop on live end.

## Requirements

### R1: Auto wait for live

User story: As a user, I want recording to wait for the streamer to go live and then start automatically.

Acceptance criteria:
1. WHEN auto mode is enabled and the streamer is not live THEN the system SHALL poll until live starts or a timeout/cancel occurs.
2. WHEN live starts THEN the system SHALL start recording within one poll interval.
3. WHEN live does not start within the configured max wait THEN the system SHALL mark the task as failed with a clear reason.

### R2: Auto stop on live end

User story: As a user, I want recording to stop automatically when the live ends.

Acceptance criteria:
1. WHEN a live session ends THEN the system SHALL stop the recording process.
2. WHEN the recording stops THEN the system SHALL remux the output to MP4.
3. THE system SHALL retain the temporary recording file if remux fails, and report the error.

### R3: Status and progress

User story: As a user, I want to see accurate task status while waiting, recording, and finalizing.

Acceptance criteria:
1. THE system SHALL expose a distinct status while waiting for live (e.g., WAITING).
2. THE system SHALL update status to RECORDING when recording starts.
3. THE system SHALL update status to COMPLETED after remux succeeds.

### R4: Configuration

User story: As a user, I want to configure polling and timeouts.

Acceptance criteria:
1. THE system SHALL support polling interval configuration (ms).
2. THE system SHALL support a max wait time configuration (ms).
3. THE system SHALL default to 120000 ms polling and 3600000 ms max wait when config is absent.

### R5: Cancellation

User story: As a user, I want to cancel a pending auto recording.

Acceptance criteria:
1. WHEN a user calls stop while in WAITING THEN the system SHALL cancel the task and stop polling.
2. WHEN a user calls stop while RECORDING THEN the system SHALL stop the recording process and finalize.

### R6: Logging

User story: As an operator, I want logs that explain auto start/stop behavior.

Acceptance criteria:
1. THE system SHALL log each poll attempt at debug level.
2. THE system SHALL log transitions between WAITING, RECORDING, and FINALIZING/COMPLETED.

## Non-Goals

- No UI changes.
- No new external services.
- No advanced scheduling (cron) beyond simple polling.
