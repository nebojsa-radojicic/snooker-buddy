# snooker-buddy

## Prerequisites
* JDK 25
* Docker and Docker Compose
* Your favourite IDE (Recommended: [IntelliJ IDEA](https://www.jetbrains.com/idea/))

Install JDK, Maven, Gradle using [SDKMAN](https://sdkman.io/)

```shell
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk install java 25-tem
# If using Gradle
$ sdk install gradle
# If using Maven
$ sdk install maven
```

## Static product mockup

The standalone mock website lives in [`mockup/`](mockup/README.md), based on
`docs/PRD.md`. Preview it without Java, authentication, or a database:

```shell
python3 -m http.server 4173 --directory mockup
```

Open `http://localhost:4173/`, or open `mockup/index.html` directly in your browser.

### Exercise library

Open **Exercise library** in the workspace navigation (`/exercises`) to save a
photo, name, setup instructions, goal, difficulty, and predefined exercise type.
Search updates as you type; difficulty and type filters update results immediately.
Name matching ignores case and surrounding spaces. Filters combine, stay in the
URL for refresh and browser navigation, and use the same six predefined types as
the exercise form. Clear search removes the name query while retaining difficulty
and type selections; Clear filters resets all three controls. Switch between grid
and list views or open an exercise to see its full drawing and instructions.
The Search button, Clear search, and type links also work without JavaScript.

Libraries and drawings are private to the signed-in account. Flyway migration
`V002` stores exercises and drawings in PostgreSQL. JPG and PNG uploads work
without JavaScript; the browser also accepts WebP, converts it to JPEG, and shows
a preview. Photos are limited to 20 MB and 60 megapixels, and saved drawings are
resized to a maximum of 1600 pixels on the longest side.

### Training assignments

Open **My players** to link an existing registered player by email, then use
**Assign training** from **Training plans** or **Assign** on a library exercise.
Assigning from a single library exercise preselects that exercise and the
**Individual exercises** format. The format can still be changed in the form.
Select a player, one or more exercises, and independent or joint practice.
**Training session** groups the selection into one named plan; **Individual
exercises** creates a separate plan named after each exercise. An unnamed grouped
plan is called “Practice session”. Each exercise has default attempts and a success
target, shown in the assignment picker alongside its success description. Each
exercise keeps its own targets in a grouped session. The existing personalization
option applies an override to all selected exercises for that assignment.

Both the coach and assigned player can open the plan and its exercise drawings
from **Training plans**. Players only gain access to exercises included in their
plans. Each player can be linked to one coach; assignment and roster changes
require sign-in and CSRF protection. Migration `V003` persists the roster and plans
in PostgreSQL. Result recording is a separate workflow.

### Coach/player access and basic scores (UC-001)

Registration supports **Coach**, **Player**, or both roles. A dual-role account can
coach several players while training under its own single coach. Coach tools
(library, roster, and assignment creation) require the coach role. Players open
assigned drawings through their training plans; no access to their coach's other
private drawings is granted. Administrator status alone does not grant coach or
player access.
The seeded `admin@gmail.com` account receives explicit Coach and Player roles
through migration `V006`. Restart the application to apply the migration, then
sign out and sign in again to refresh the session authorities.

Migration `V004` replaces the single authority column with `user_roles`, preserving
legacy authorities and inferring coach/player roles from existing exercise
ownership and roster links. Existing accounts with no such records retain only
their previous authority; the migration does not grant them business privileges.

Migration `V005` stores basic exercise scores. **Scores** (`/scores`) lists the
signed-in player's scores and scores for players they coach. Direct reads at
`GET /scores/{id}` return JSON; inaccessible or unknown scores return 404. Counts,
player, exercise, plan, and recording timestamp are included.

The basic storage endpoint is
`POST /training/{planId}/exercises/{exerciseId}/scores` with JSON
`{"total":10,"successful":7}`. It requires an authenticated session and CSRF token;
only the coach can submit joint practice, and only the assigned player can submit
independent practice. Invalid counts return 400. Scores must reference an exercise
in that plan. A successful write returns 201 with the score and a `Location`
header. This endpoint is the storage foundation; detailed result-entry screens,
attempt-by-attempt entry, and optional performance measures belong to later use
cases.

### Default exercise targets (UC-007)

The new-exercise form accepts default attempts (1–1000) and a successful-attempt
target (1 through the total attempts). The existing **What counts as success?**
text describes how to judge each attempt. Targets start at 8 successes out of 10;
these values also apply to older exercises after migration `V007`.

Library details and assignment preparation show each exercise's defaults.
Assignments copy the targets into each included exercise, so later library changes
cannot change an existing assignment. `V007` preserves the counts on existing
plans by copying them to their exercise entries; existing scores stay linked.
