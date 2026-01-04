# Tasks

## Implementation Steps

1. Update TaskStatus
   - Add WAITING (and optional FINALIZING).

2. Update RecordingTask
   - Add autoEnabled (if needed for per-task control).

3. Add configuration
   - recording.poll-interval-ms
   - recording.max-wait-ms
   - recording.end-detect-grace-ms
   - recording.auto-enabled (default true)

4. Add request parameter
   - Add optional `auto` field to StartRecordingRequest.
   - Default to config when not provided.

5. Implement wait-for-live loop
   - In RecordingManager.executeTask, add polling when auto-enabled.
   - Transition WAITING -> RECORDING when live starts.
   - Fail when max wait exceeded.

6. Implement auto stop on live end
   - During recording, periodically check live status.
   - If live ends, call stopTask and finalize remux.

7. Update stop behavior
   - If stop called while WAITING, cancel and exit.

8. Logging
   - Add debug logs for polling and state transitions.

9. Tests
   - Unit tests for polling transitions and max wait timeout.
   - Unit test for stop during WAITING.
   - Integration test for auto stop on live end.

## Notes

- WAITING tasks should not count toward max concurrent recordings.
- Preserve current /start behavior when auto mode is disabled.
