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
the exercise form. Clear filters resets all three controls. Switch between grid
and list views or open an exercise to see its full drawing and instructions.
The Search button and type links also work without JavaScript.

Libraries and drawings are private to the signed-in account. Flyway migration
`V002` stores exercises and drawings in PostgreSQL. JPG and PNG uploads work
without JavaScript; the browser also accepts WebP, converts it to JPEG, and shows
a preview. Photos are limited to 20 MB and 60 megapixels, and saved drawings are
resized to a maximum of 1600 pixels on the longest side.

### Training assignments

Open **My players** to link an existing registered player by email, then use
**Assign training** from **Training plans** or **Assign** on a library exercise.
Select a player, one or more exercises, and independent or joint practice.
**Training session** groups the selection into one named plan; **Individual
exercises** creates a separate plan named after each exercise. An unnamed grouped
plan is called “Practice session”. Targets default to 8 successful attempts out
of 10 and can be customized for the selected player.

Both the coach and assigned player can open the plan and its exercise drawings
from **Training plans**. Players only gain access to exercises included in their
plans. Each player can be linked to one coach; assignment and roster changes
require sign-in and CSRF protection. Migration `V003` persists the roster and plans
in PostgreSQL. Result recording is a separate workflow.
