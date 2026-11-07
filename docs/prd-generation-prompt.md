Act as an experienced product manager.

Below is the summary of a brainstorming session for a product called **Snooker Buddy**.
Write a Product Requirements Document (PRD) in Markdown with exactly these sections:

1. Overview
2. Problem Statement
3. Goal / Vision
4. Target Users
5. Key Features (MVP) - a short list, one line each
6. Out of Scope
7. Success Metrics
8. Risks and Assumptions
9. Open Questions

Rules:
- Do not include technical details, architecture, or tech stack.
- Do not write detailed requirements, user stories, or acceptance criteria yet.
- Do not invent features that are not in the summary. If something is unclear or missing, put it under Open Questions instead of guessing.
- Keep it concise: one or two pages at most. Plain language, no marketing fluff.
- Before writing, ask me any clarifying questions you have, one at a time. Once I answer, generate the full document.
- Save the result as `docs/PRD.md`.

## Brainstorming summary

## What we are going to build

A simple application for snooker coaches and players to organize exercises, assign training, and record results in one place.

The core workflow is: **save an exercise → assign it to a player → complete the exercise → record and review results**.

The initial focus is making it easy for coaches to save their own exercises. A photo of an existing drawing is sufficient for the first version; a digital table editor is not required.

## Vision / goal

Create continuity between coached sessions and independent practice:

- Coaches retain an organized, reusable collection of their own exercises.
- Players know what to practice and how to perform each exercise without relying on memory.
- Coaches can use training history and recorded results to inform future sessions.
- Both can observe changes in performance on repeated exercises over time.

Simplicity is the intended differentiator. The product should make routine tasks easier without overwhelming users with unnecessary options. This advantage still needs to be demonstrated against existing applications.

## Problem

Coaches currently set up exercises on the table, sometimes draw them, and explain instructions verbally. Drawings get lost or mixed together, and players forget the exact exercise or its instructions.

When a coach is unavailable, a player may practice a different exercise they remember. Neither party has a reliable record of which exercises were completed or what results were achieved. Coaches therefore rely on memory, subjective impressions, and improvisation when planning subsequent training.

According to the founder, approximately ten coaches independently reported problems with storing exercises and tracking player progress. Coaches and players have expressed strong willingness to participate in a pilot lasting several weeks.

Some coaches have tried existing applications. Reported objections include price, limited influence over product changes, and complicated interfaces with unnecessary detail. Saving an exercise was identified as a particularly difficult task; the exact obstacles in that workflow still need to be observed.

Existing products overlap with the concept. The opportunity is therefore to serve these users better, not to assume that no competing solution exists. Willingness to pay remains unvalidated.

## Target users

- **Snooker coaches:** maintain their own exercise collection, prepare training, assign exercises, and review player results.
- **Snooker players working with a coach:** access assigned exercises and record independent practice.

For the MVP, one coach can work with multiple players, but each player works with only one coach. Coaches use their own private exercise collections; players access exercises assigned to them.

## MVP scope

- **Exercise library:** create and save exercises using a photo of a drawing, a name, a description, a goal, a difficulty label, and an exercise type.
- **Search and filtering:** search by name and filter by difficulty and type.
- **Shared exercise types:** use a predefined list covering technique, cue-ball control, positional play, tactics, safety play, mental preparation, and other agreed areas.
- **Assignments:** assign one or more exercises individually for independent practice or group them into a training session.
- **Targets:** use an exercise's default targets or adapt the required attempts and success criteria for a particular player.
- **Results:** record successful attempts out of total attempts, with optional consecutive-success and time-based measures.
- **Result entry:** support both attempt-by-attempt entry and a summary entered after the exercise.
- **Responsibility:** coaches enter results for joint sessions; players enter results for independent sessions.
- **History:** retain separate records of repeated exercise performances so coaches and players can review completed work and results.

Difficulty labels support organization and filtering; they do not represent a validated player-level system. The exact difficulty scale and interface details remain to be finalized.

## Out of scope for now

- Automatic player categorization or promotion between skill levels.
- Standardized progression criteria; these would need to be developed and validated with coaches.
- Collaboration between one player and multiple coaches.
- Sharing exercise libraries between coaches.
- A digital snooker-table editor.
- Coach-defined exercise types; the MVP uses a shared predefined list.

Custom development on demand is not an agreed product commitment. Feedback should help identify common needs rather than imply that every individual request will be implemented.

## Success signal

The primary outcome stated by coaches is: **their exercises are organized in the application, and preparing training is straightforward**.

Observable signs during the pilot would include:

- Coaches save and reliably retrieve the exercises they need.
- Coaches assign training without reconstructing lost exercises or instructions.
- Players can independently understand and repeat assigned exercises.
- Coaches and players consistently record results, producing useful training history.
- Coaches use that history when preparing subsequent training.
- Users continue using the application throughout the pilot and want to keep using it afterward.

These are proposed observations, not yet agreed numerical targets. Pilot participation indicates interest; sustained use and willingness to pay still need to be tested. Improved exercise results alone should not be treated as proof of an overall increase in player skill.
