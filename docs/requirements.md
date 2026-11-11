# Snooker Buddy Requirements

This document translates the MVP in [PRD.md](PRD.md) into implementable use cases. [The mockup](../mockup/index.html), its [workflow notes](../mockup/README.md), and its [workflow checks](../mockup/tests/workflows.cjs) inform the interaction descriptions. The PRD remains the authority for scope; prototype choices are not automatically product requirements. All use cases start as `NOT_IMPLEMENTED`; this is a planning status, not an audit of existing code. Other permitted statuses are `IN_PROGRESS` and `COMPLETED`.

Excluded scope: automatic player categorization or promotion, standardized progression criteria, multiple coaches per player, exercise-library sharing between coaches, a digital table editor, and coach-defined exercise types. Difficulty labels organize exercises and do not establish validated player levels. Optional consecutive-success and time measures are not mandatory MVP use cases; their definitions remain open in the PRD. Pilot metrics are evaluation indicators, not an implicit requirement for an analytics subsystem.

The actors below are an identified coach and an identified player with an existing coach–player relationship. Account provisioning, sign-in, invitations, and relationship changes are unspecified; these use cases do not prescribe those workflows.

## Use Case Summary

| UseCase ID | Use Case Title | Status |
|---|---|---|
| UC-001 | Enforce Coach and Player Access Boundaries | COMPLETED |
| UC-002 | Save an Exercise in a Private Library | COMPLETED |
| UC-003 | View Exercise Instructions and Goals | COMPLETED |
| UC-004 | Search Exercises by Name | COMPLETED |
| UC-005 | Filter Exercises by Difficulty | COMPLETED |
| UC-006 | Filter Exercises by Predefined Type | COMPLETED |
| UC-007 | Define Default Exercise Targets | COMPLETED |
| UC-008 | Assign an Individual Exercise | COMPLETED |
| UC-009 | Assign a Grouped Training Session | COMPLETED |
| UC-010 | Personalize Targets for a Player | COMPLETED |
| UC-011 | Review Assigned Training | COMPLETED |
| UC-012 | Enforce Result Recording Ownership | NOT_IMPLEMENTED |
| UC-013 | Record an Exercise Result as a Summary | NOT_IMPLEMENTED |
| UC-014 | Record an Exercise Result Attempt by Attempt | NOT_IMPLEMENTED |
| UC-015 | Review Completed Training and Results | NOT_IMPLEMENTED |
| UC-016 | Review Separate Performances of a Repeated Exercise | NOT_IMPLEMENTED |

## Detailed Use Cases

### UC-001 — Enforce Coach and Player Access Boundaries

- **Status:** COMPLETED
- **Depends On:** None
- **Description:** A coach works with their own private exercise collection and multiple associated players; each player accesses their own assigned training and results under one coach. The mockup's coach/player switch illustrates these perspectives, without specifying a production identity workflow.
- **Acceptance Criteria:**
  - An account can hold the coach role, the player role, or both roles simultaneously.
  - A coach can work with more than one associated player.
  - A player cannot have more than one coach in the MVP.
  - A coach cannot access another coach's private exercise collection.
  - A player can access exercises assigned to them without gaining access to the coach's unassigned private exercises.
  - A player cannot access another player's assignments or results.
  - A coach can review results for their associated players without gaining access to unrelated players' results.
  - Basic scores persist successful and total attempt counts against the assigned exercise and player.
  - A player can read their own scores; unrelated players and coaches cannot read them through lists or direct score identifiers.

Implementation verification: coach/player role boundaries, combined roles, basic score persistence, and restricted score reads are implemented. Registration handles omitted role checkboxes correctly; all five focused MVC regression tests passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS` after the registration fix.

### UC-002 — Save an Exercise in a Private Library

- **Status:** COMPLETED
- **Depends On:** UC-001
- **Description:** A coach saves an existing exercise using a photo of a drawing and descriptive metadata so it can be found and reused. The mockup's New exercise form illustrates the workflow; a table editor is not required.
- **Acceptance Criteria:**
  - A coach can save an exercise containing a drawing photo, name, description, goal, difficulty label, and exercise type.
  - The saved exercise belongs to the creating coach's private collection.
  - The saved drawing photo remains available when the exercise is reopened.
  - Reopening a saved exercise displays its stored metadata.
  - Saving an exercise does not require assigning it to a player.
  - Saving an exercise does not require recreating its drawing with a digital table editor.
  - A player cannot create an exercise in the coach's private collection.

Implementation verification: the existing photo-upload and private-library flow implements the required behavior; no production changes were necessary. Regression coverage now checks all metadata, authenticated ownership despite a forged owner field, saving without a coach–player link or training assignment, reopening the drawing, missing uploads, and rejection of player-only writes. Eleven focused MVC/upload/drawing tests passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database and full-build verification.

### UC-003 — View Exercise Instructions and Goals

- **Status:** COMPLETED
- **Depends On:** UC-002
- **Description:** A coach, or a player with access to an assigned exercise, opens its details to understand the setup, instructions, and intended goal. The mockup presents these as the drawing, How to practice, and What counts as success.
- **Acceptance Criteria:**
  - An authorized viewer can open a saved exercise and see its drawing photo.
  - Exercise details display the name, description, and goal.
  - Exercise details display the difficulty label and predefined type.
  - Viewing an exercise does not alter its saved content.
  - Exercise difficulty is presented as an exercise label, without assigning the player a validated skill level.

Implementation verification: coach-library and assigned-exercise views display the drawing, name, instructions, goal, and explicitly labeled exercise type and difficulty. Added MVC tests for both viewing paths and authorization failures, plus a database snapshot assertion that viewing leaves the stored exercise unchanged. Fourteen focused tests passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database and full-build verification.

### UC-004 — Search Exercises by Name

- **Status:** COMPLETED
- **Depends On:** UC-002
- **Description:** A coach searches the private collection by exercise name to retrieve an exercise for training preparation. The mockup provides a name-search field and a no-results state.
- **Acceptance Criteria:**
  - A coach can enter a name query to find exercises by name.
  - Search results contain only exercises within that coach's collection.
  - A query with no matching exercise produces an observable no-results outcome.
  - Clearing the query removes the name restriction.
  - Searching does not modify saved exercises.

Implementation verification: name search uses the existing case-insensitive, whitespace-trimmed substring match within the authenticated coach's library. Added a Clear search action that retains difficulty/type selections and works with ordinary navigation or HTMX. Regression tests cover owner identity, no-results rendering, clearing, access denial, query normalization, cross-coach isolation, and unchanged stored exercises. Eight focused tests passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database and full-build verification.

### UC-005 — Filter Exercises by Difficulty

- **Status:** COMPLETED
- **Depends On:** UC-002
- **Description:** A coach narrows the exercise collection using difficulty labels. The mockup's difficulty selector illustrates the interaction; its Easy, Medium, and Hard labels are illustrative pending the PRD decision.
- **Acceptance Criteria:**
  - A coach can select a difficulty label to restrict results to exercises with that label.
  - A difficulty selection with no matching exercises produces an observable no-results outcome.
  - Clearing the difficulty selection removes the difficulty restriction.
  - Filtering never introduces exercises from another coach's collection.
  - Filtering by difficulty does not categorize or promote a player.

Implementation verification: the existing difficulty selector and owner-scoped query implement the required behavior; no production changes were necessary. Added focused coverage for each existing label, selected-state retention, clearing through All difficulties while retaining name/type filters, empty results, and coach-only access. Database regressions check isolation between coaches and unchanged user roles and exercise data. Six focused MVC checks passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database and full-build verification.

### UC-006 — Filter Exercises by Predefined Type

- **Status:** COMPLETED
- **Depends On:** UC-002
- **Description:** A coach classifies and retrieves exercises using a shared predefined type list. The mockup's type tabs illustrate retrieval, while the final type vocabulary remains open.
- **Acceptance Criteria:**
  - Exercise creation offers types from a shared predefined list.
  - A coach can select a predefined type to restrict results to exercises of that type.
  - A type selection with no matching exercises produces an observable no-results outcome.
  - Clearing the type selection removes the type restriction.
  - A coach cannot create a custom exercise type.
  - All coaches use the same predefined type list.

Implementation verification: the existing shared type enum, creation selector, type tabs, and owner-scoped query implement the required behavior; no production changes were necessary. Regression coverage checks all predefined types, shared creation/retrieval choices across coaches, selected tabs, empty results, clearing only the type restriction, cross-coach isolation, and rejection of custom type values without persistence. Eleven focused MVC checks passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database and full-build verification.

### UC-007 — Define Default Exercise Targets

- **Status:** COMPLETED
- **Depends On:** UC-002
- **Description:** A coach supplies default attempts and success criteria for an exercise so it can be assigned without re-entering the target. The mockup distinguishes library defaults from personal targets.
- **Acceptance Criteria:**
  - A coach can define the default number of attempts for an exercise.
  - A coach can define the default success criteria for an exercise.
  - The exercise retains its default targets for subsequent assignments.
  - Defaults are available for inspection when preparing an assignment.
  - Each exercise keeps its own default targets when included in a grouped session.
  - Where success is expressed as a count of successful attempts, the target cannot exceed the planned total attempts.

Implementation verification: exercise creation accepts default attempts and successful-attempt targets alongside the existing text success criterion. Defaults are persisted and shown in library details and assignment preparation. Each assignment stores targets per exercise, including grouped sessions; existing personalization overrides those copies without changing library defaults. Migration `V007` backfills existing exercise defaults to 8/10 and copies existing plan targets into their exercise entries while retaining score links. Eighteen focused tests passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database, migration, and full-build verification.

### UC-008 — Assign an Individual Exercise

- **Status:** COMPLETED
- **Depends On:** UC-003, UC-007
- **Description:** A coach assigns an exercise from their collection to an associated player for joint or independent practice. The mockup's Assign action provides an entry point from an exercise card or detail view.
- **Acceptance Criteria:**
  - A coach can select an exercise from their own collection and assign it to an associated player.
  - The assignment identifies the exercise and recipient player.
  - The assignment distinguishes joint practice from independent practice for result ownership.
  - The assignment uses the exercise's default targets when no personalized targets are selected.
  - The recipient can access the assigned exercise's drawing, instructions, goal, and applicable targets.
  - A coach cannot assign another coach's private exercise.
  - A coach cannot assign training to a player outside their coach–player relationships.
  - A player cannot issue a coach's training assignment.

Implementation verification: a single exercise selected from the library now defaults to the individual-assignment format. Existing assignment logic preserves the recipient, joint/independent practice kind, exercise identity, and its default targets. Added MVC and service coverage for both practice kinds, coach identity, ownership/roster rejection, missing selections, player-only rejection, and CSRF protection. Database regressions verify persistence and recipient access to instructions, goals, drawings, and targets, with rejected requests leaving no assignments. Eleven focused checks passed. The user confirmed `./mvnw clean verify` finished with `BUILD SUCCESS`, completing database and full-build verification.

### UC-009 — Assign a Grouped Training Session

- **Status:** COMPLETED
- **Depends On:** UC-003, UC-007
- **Description:** A coach groups exercises into one training session and assigns the session to an associated player. The mockup distinguishes Training session from Individual exercises.
- **Acceptance Criteria:**
  - A coach can group multiple exercises from their collection into one assigned training session.
  - A grouped session identifies its recipient player and included exercises.
  - The session distinguishes joint practice from independent practice for result ownership.
  - Each exercise in the session has applicable attempts and success criteria.
  - The player can access each included exercise's drawing, description, and goal.
  - An assignment containing no exercises cannot be created.
  - Grouping exercises does not merge their separately recorded performances into one exercise result.

Implementation verification covers both joint and independent grouped assignments through the existing training form. The new database-backed regression checks one session with the intended recipient, each exercise's own targets, access to the assigned drawings and instructions, and distinct saved scores for each exercise. Existing tests cover empty selections. Twenty focused unit and MVC checks passed. The user confirmed that the local full build finished with `BUILD SUCCESS`.

### UC-010 — Personalize Targets for a Player

- **Status:** COMPLETED
- **Depends On:** UC-008
- **Description:** A coach retains default targets or adjusts attempts and success criteria for a particular player. The mockup shows a personalization option during assignment and carries the applicable targets into exercise and recording views.
- **Acceptance Criteria:**
  - A coach can assign an exercise using its default attempts and success criteria.
  - A coach can adjust the planned attempts for the recipient player.
  - A coach can adjust the required number of successes and optionally override the written success criterion for each assigned exercise. A blank written override uses the library criterion.
  - Personalized targets are associated with that player's assigned exercise.
  - Personalizing a player's assignment does not change another player's targets or the exercise's library defaults.
  - The assigned exercise displays the applicable personalized targets instead of the defaults where overrides exist.
  - A target expressed as successful attempts cannot exceed the assigned total attempts.
  - Players cannot change the targets set by their coach through the assignment workflow.

Implementation verification covers optional written overrides on individual and grouped assignments. Each nonblank criterion is stored with its assigned exercise and shown on the plan and exercise views. The form uses the library's 2,000-character limit and rejects overrides for unselected exercises. Tests cover binding distinct criteria, retained numeric validation, player access restrictions, and isolation from other assignments and library defaults. The local run passed 118 tests before Docker access blocked the database-backed cases. The user confirmed that the full local build finished with `BUILD SUCCESS`, completing database and migration verification.

### UC-011 — Review Assigned Training

- **Status:** COMPLETED
- **Depends On:** UC-008, UC-009
- **Description:** A player reviews their assignments before practice, and a coach reviews training prepared for associated players. The mockup's Training plans and My training views provide entry points to assignment details.
- **Acceptance Criteria:**
  - A player can retrieve their assigned individual exercises and any assigned grouped sessions.
  - A coach can retrieve assignments for their associated players.
  - Opening an assignment identifies its recipient and whether practice is joint or independent.
  - The assignment makes each included exercise's instructions, drawing, goal, and applicable targets available.
  - If an exercise appears in multiple assignments, the assignment being reviewed determines which personalized targets are shown.
  - A player cannot retrieve another player's training through this workflow.

Implementation verification covers the existing training list, plan details, and exercise views. Added regression coverage for coach and player review, roster isolation, and plan-specific targets and success criteria when the same exercise appears in multiple assignments. Thirteen focused unit and MVC checks passed in the agent environment. The user confirmed `BUILD SUCCESS` in their local environment, completing verification.

### UC-012 — Enforce Result Recording Ownership

- **Status:** NOT_IMPLEMENTED
- **Depends On:** UC-011
- **Description:** The authorized recorder records a player's performance in its practice context: the coach records joint sessions and the player records independent sessions. The mockup indicates the responsible recorder and retains assignment context through result entry.
- **Acceptance Criteria:**
  - A coach can record a joint-session result for an associated player.
  - A player can record their own independent-session result.
  - A player cannot record a joint-session result through the player workflow.
  - A coach cannot record an independent-session result through the coach workflow.
  - A player cannot record a result for another player.
  - Result entry identifies the player and exercise being performed.
  - Result entry from an assignment preserves that assignment's context and applicable targets.
  - An exercise not included in the selected assignment cannot receive a result under that assignment.
  - Rejecting an unauthorized recording does not add a performance to history.

### UC-013 — Record an Exercise Result as a Summary

- **Status:** NOT_IMPLEMENTED
- **Depends On:** UC-012
- **Description:** The authorized recorder enters successful versus total attempts after performing an exercise. The mockup's Summary mode illustrates this entry path.
- **Acceptance Criteria:**
  - The authorized recorder can enter the total attempts performed and the successful attempts for one exercise performance.
  - The recorded actual total is distinguishable from the planned attempts target.
  - Total attempts must be a positive whole number.
  - Successful attempts must be a whole number between zero and the actual total, inclusive.
  - A summary with zero successes and a positive total can be saved.
  - Invalid counts prevent saving and identify the validation problem.
  - A valid saved summary retains its player, exercise, and practice context for history review.
  - Recording successful versus total attempts does not require entering optional consecutive-success or time measures.

### UC-014 — Record an Exercise Result Attempt by Attempt

- **Status:** NOT_IMPLEMENTED
- **Depends On:** UC-012
- **Description:** The authorized recorder records each attempt as successful or unsuccessful during exercise performance. The mockup's Attempt by attempt mode illustrates the alternative to summary entry.
- **Acceptance Criteria:**
  - The authorized recorder can register an individual attempt as successful or unsuccessful.
  - Each registered attempt increments the actual total by one.
  - A successful attempt increments the successful count by one.
  - An unsuccessful attempt leaves the successful count unchanged.
  - The recorder can see the accumulated successful and total attempts before saving.
  - A performance containing only unsuccessful attempts can be saved once at least one attempt exists.
  - A performance containing no attempts cannot be saved as a completed result.
  - Saving retains the accumulated counts with the player, exercise, and practice context for history review.
  - Attempt entry does not require entering optional consecutive-success or time measures.

### UC-015 — Review Completed Training and Results

- **Status:** NOT_IMPLEMENTED
- **Depends On:** UC-013, UC-014
- **Description:** Coaches and players review recorded work and exercise results in training history. The mockup provides Training history, My results, and a player-specific history entry point from My players.
- **Acceptance Criteria:**
  - A saved performance is available in training history after either supported recording mode is used.
  - Each history performance identifies the player and exercise and displays successful versus total attempts.
  - The history makes joint and independent practice distinguishable.
  - A player can review their own recorded performances.
  - A coach can review recorded performances of associated players.
  - Reviewing history does not alter stored performance counts.
  - A player with no recorded performances has no fabricated results displayed.
  - Recorded results describe exercise performance without assigning an overall player category or promotion.

### UC-016 — Review Separate Performances of a Repeated Exercise

- **Status:** NOT_IMPLEMENTED
- **Depends On:** UC-015
- **Description:** A coach or player reviews multiple performances of the same exercise to understand performance over repeated practice. The mockup preserves repeated results as separate history entries.
- **Acceptance Criteria:**
  - Recording a later performance of an exercise creates a separate performance rather than replacing an earlier result.
  - Repeated performances remain associated with the correct player and exercise.
  - Authorized viewers can inspect the successful and total attempts for each performance of the same exercise.
  - Results from different players are not conflated as one player's repeated performance.
  - Reviewing repeated exercise results does not infer validated overall playing ability or automatic skill-level advancement.

## Open Decisions and Mockup Alignment

The following mockup details are references for discussion, not additional acceptance criteria or approved extensions of the PRD:

| Topic | Mockup behavior | Decision needed |
|---|---|---|
| Difficulty and types | Easy/Medium/Hard and six example exercise types. | Confirm the final difficulty scale and shared type list, as requested by the PRD. |
| Drawing uploads | JPG, PNG, and WebP; 20 MB input limit; automatic resizing and preview. | Confirm accepted formats, limits, image-quality expectations, and upload-failure behavior. The PRD requires a drawing photo but specifies none of these policies. |
| Text and count limits | Name length and numeric limits are set in forms. | Confirm field limits, requiredness beyond the specified exercise content, matching rules for search, and the permitted attempt/target range. |
| Assignment targets | One override applies to all selected exercises; otherwise each exercise uses its own default. | Confirm whether grouped sessions need independent numeric overrides for each exercise. Written success criteria can be overridden per assigned exercise. |
| Assignment selection | A player chooses a plan when an exercise belongs to several plans. | Confirm selection behavior and whether unassigned practice can be recorded. No standalone recording workflow is specified here. |
| Completion | A plan becomes Completed once each included exercise has at least one result, regardless of target attainment; further results remain possible. | Define completion, partial work, repeated recording after completion, and whether actual attempts may differ from planned attempts. The PRD requires completed-work review but does not settle these transitions. |
| Consecutive success and time | Optional best streak and duration in minutes; attempt mode calculates a streak. | Define these optional measures before adding normative use cases; the PRD explicitly leaves their definitions open. |
| History context | New results retain plan identity, planned attempts, target, recorder, and date. | Confirm timestamps, ordering, historical target snapshots, and behavior if exercises or assignments change. Do not infer edit/delete workflows from this gap. |
| Search and filtering | Name matching is case-insensitive; name, difficulty, and type restrictions can be combined. | Confirm matching and combination semantics; the PRD names the retrieval dimensions without prescribing these rules. |
| Overview statistics | Player cards and history show counts and aggregate logged-attempt success. | Confirm whether these summaries are required; aggregated statistics are not independent MVP commitments in this specification. |
| Prototype controls and persistence | Role switching, sample players, browser-local storage, Reset demo, and temporary storage fallback. | These support the static demonstration. They do not define production identity, player onboarding, storage, or data-loss behavior. |

The PRD's remaining pilot questions—duration, participant count, metric targets, consistent baseline measurement, exercise-saving friction, and willingness to pay—remain product-validation questions rather than additional functional use cases.
