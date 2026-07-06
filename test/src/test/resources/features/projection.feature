Feature: Projections select events by category

  A projection admin store creates a projection that copies events into a dedicated
  projection stream based on the categories the events carry in their metadata. This
  feature only runs on backends that expose a projection admin store (jpa, esgrpc);
  the in-memory and asynchronous KurrentDB backends self-skip via the gate step.

  The category names below are unique to this feature (and distinct per scenario), so
  the "from all" projections stay isolated on the KurrentDB container that is shared
  across scenarios.

  Scenario: A projection selects events by a single category
    Given the backend supports projections
    And the following streams don't exist
      | Stream Name    |
      | ProjCatSingle1 |
      | ProjCatSingle2 |
      | ProjCatSingle3 |
    When I append the following events in the given order
      | Stream Name    | Expected Version   | Event Id                             | Categories |
      | ProjCatSingle1 | NO_OR_EMPTY_STREAM | 11111111-1111-1111-1111-111111111111 | Borrowed   |
      | ProjCatSingle2 | NO_OR_EMPTY_STREAM | 22222222-2222-2222-2222-222222222222 | Reserved   |
      | ProjCatSingle3 | NO_OR_EMPTY_STREAM | 33333333-3333-3333-3333-333333333333 | Borrowed   |
    And I create the following projections
      | Name               | Enable | Event Types | Categories |
      | BorrowedProjection | true   |             | Borrowed   |
    Then reading forward from projection should have the following results
      | Projection Name    | Start | Count | Result Event Id 1                    | Result Event Id 2                    |
      | BorrowedProjection | 0     | 10    | 11111111-1111-1111-1111-111111111111 | 33333333-3333-3333-3333-333333333333 |

  Scenario: A projection selects events by multiple categories
    Given the backend supports projections
    And the following streams don't exist
      | Stream Name   |
      | ProjCatMulti1 |
      | ProjCatMulti2 |
      | ProjCatMulti3 |
    When I append the following events in the given order
      | Stream Name   | Expected Version   | Event Id                             | Categories |
      | ProjCatMulti1 | NO_OR_EMPTY_STREAM | aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa | Shipped    |
      | ProjCatMulti2 | NO_OR_EMPTY_STREAM | bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb | Delivered  |
      | ProjCatMulti3 | NO_OR_EMPTY_STREAM | cccccccc-cccc-cccc-cccc-cccccccccccc | Cancelled  |
    And I create the following projections
      | Name                       | Enable | Event Types | Categories         |
      | ShippedDeliveredProjection | true   |             | Shipped, Delivered |
    Then reading forward from projection should have the following results
      | Projection Name            | Start | Count | Result Event Id 1                    | Result Event Id 2                    |
      | ShippedDeliveredProjection | 0     | 10    | aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa | bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb |
