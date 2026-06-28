Feature: Subscribe to a stream

  Scenario: Catch-up subscription receives existing events from the beginning
    Given the following streams don't exist
      | Stream Name    |
      | SubscribeBasic |
    When I append the following events in the given order
      | Stream Name    | Expected Version   | Event Id                             | Expected Exception |
      | SubscribeBasic | NO_OR_EMPTY_STREAM | 11111111-1111-1111-1111-111111111111 | -                  |
      | SubscribeBasic | 0                  | 22222222-2222-2222-2222-222222222222 | -                  |
    Then this should raise no exception
    When I subscribe to stream "SubscribeBasic" from the beginning
    Then the subscription should receive the following events
      | Event Id                             |
      | 11111111-1111-1111-1111-111111111111 |
      | 22222222-2222-2222-2222-222222222222 |

  Scenario: Catch-up subscription receives events starting from a later event number
    Given the following streams don't exist
      | Stream Name    |
      | SubscribeFromN |
    When I append the following events in the given order
      | Stream Name    | Expected Version   | Event Id                             | Expected Exception |
      | SubscribeFromN | NO_OR_EMPTY_STREAM | 33333333-3333-3333-3333-333333333333 | -                  |
      | SubscribeFromN | 0                  | 44444444-4444-4444-4444-444444444444 | -                  |
      | SubscribeFromN | 1                  | 55555555-5555-5555-5555-555555555555 | -                  |
    Then this should raise no exception
    When I subscribe to stream "SubscribeFromN" from event number 1
    Then the subscription should receive the following events
      | Event Id                             |
      | 44444444-4444-4444-4444-444444444444 |
      | 55555555-5555-5555-5555-555555555555 |
