# SRS Review Report

## 1. Review Overview

### Review Target
Software Requirements Specification for **Choose Your Fate**

### Review Type
Formal review of SRS document

### Manager
Underviser

### Purpose of Review
The purpose of this review is to identify defects, ambiguities, inconsistencies, omissions, and quality issues in the SRS before the document is finalized and delivered.

## 2. Participants and Roles

| Name | Role | Responsibility |
|---|---|---|
| Underviser | Manager | Decides what is reviewed, appoints participants, sets budget and timeframe |
| Sebastian | Author | Author of the SRS being reviewed |
| Patrick | Moderator / Review Leader | Plans the review, follows up, mediates, writes the review report |
| Mikkel | Reviewer | Reviews the SRS and raises issues |
| Alexander | Scribe / Recorder | Documents issues found during the review |

## 3. Review Logistics

| Field | Value |
|---|---|
| Review date | 19/05/2026 |
| Review period / meeting date | 8:15:00 |
| Duration | Finished 11:15:00 / 3 Hours |
| Review format | In person roundtable discussion |
| Document version reviewed | SRS |
| Work product size | 13903 / 5½ standard pages |

## 4. Review Method

The review was carried out as a formal document review. The reviewers examined the SRS with focus on:

- Completeness
- Correctness
- Clarity
- Consistency
- Testability
- Alignment with Software Quality course requirements

## 5. Severity Scale

| Severity | Meaning | Color |
|---|---|---|
| Critical | Major defect that makes the SRS incorrect, unusable, or misleading in an essential area | Red |
| Major | Important defect that should be corrected before final submission | Orange |
| Minor | Limited defect such as wording, clarity, formatting, or small inconsistency | Yellow |
| Suggestion | Improvement suggestion, not necessarily a defect | Green |

## 6. Defect Summary

| Severity | Count |
|---|---|
| Critical | 8 |
| Major | 13 |
| Minor | 7 |
| Suggestion | 9 |
| Total | 37 |

## 7. Defect Log

| ID | Section | Description of Issue | Severity | Raised By | Recorded By | Recommended Action | Status |
|---|---|---|---|---|---|---|---|
| R1 | Course Context | Using "the" feels impersonal for the project | Suggestion | Patrick | Alexander | Change "the" to "our" | Accepted |
| R2 | Course Context | Inconsistency in naming conventions of the requirements | Major | Patrick | Alexander | Change "quality requirements" to "non-functional requirements" | Accepted |
| R3 | Course Context | "current version of the system" this should be for all versions | Minor | Mikkel | Alexander | Remove current version statement | Accepted |
| R4 | Course Context | The last sentence feels incoherent | Major | Sebastian | Alexander | Complete rewrite of the sentence: This document reflects the team's current goal of creating a vertical slice of the software, that can be expanded upon further later. | Accepted |
| R5 | 1.2 Intended Audience | More accurate audience description | Suggestion | Sebastian | Alexander | Add "Backers of the project (in this case teachers and examiners)" | Accepted |
| R6 | 1.3 Product Scope | Fallback should be added to the overall product scope and the short-term project goal | Major | Patrick | Alexander | Will add fallback to Product Scope and short-term project goal areas in the document | Accepted |
| R7 | 1.3 Product Scope | "Advanced social features such as chat, guilds, multiplayer sessions, or leaderboards" is not as feature that will be implemented | Major | Sebastian | Alexander | Delete the line | Accepted |
| R8 | 1.4 Definitions | A sceneId can redirecton to the same sceneId not only new scenes | Minor | Mikkel | Alexander | Changed from "another" to "another or the same sceneId" | Accepted |
| R9 | 2.1 Product Perspective | "For the Software Quality version of the project" implies there are multiple versions of the project | Suggestion | Patrick | Alexander | Remove "Software Quality" from the line | Accepted |
| R10 | 2.2 User Classes | "Potentially" and "quest" should be removed the requirements should be specific | Minor | Patrick | Alexander | Remove "Potentially" and change "quests" to equipment | Accepted |
| R11 | 2.2 User Classes (Administrator) | "Potentially" and "other entities" should be removed the requirements should be specific | Minor | Patrick | Alexander | Remove "Potentially" and add equipment plus change all "Game entities" to "ETC...(All CRUD)"  | Accepted |
| R12 | 3.1 Assumptions | "in the Software Quality version" is not relevant | Suggestion | Sebastian, Mikkel and Patrick | Alexander | Remove "in the Software Quality version" from line 99 | Accepted |
| R13 | 3.2 Constraints | "and then one full demonstrable adventure" Does not make sense to write "limited to" and then add more afterwards | Major | Sebastian and Mikkel | Alexander | Delete "and then one full demonstrable adventure" from the line 106 | Accepted |
| R14 |3.2 Constraints | "heterogeneous" is a word that is unnatural so lets use a simplified version of it for better understanding | Suggestion | Sebastian | Alexander | Delete everything after "setup" because it doesn't make sense for the project | Accepted |
| R15 | FR-1 Account Registration | Required credential and profile data should be as specific as possible | Critical | Mikkel | Alexander | Write the specific required credentials and profile data | Open |
| R16 | FR-3 Authorization | We don't have a player role we have a user role | Major | Sebastian | Alexander | Change "Player" to "USER" and "Administrator" to "ADMIN" | Accepted |
| R17 | FR-12 Story Structure | Specify exactly what needs to be stored | Critical | Patrick | Alexander | Re-write the specifications as specific as possible | Open |
| R18 | FR-14 Inventory Support | Remove the "if" statement as this functionality is already included | Critical | Patrick | Alexander | Remove "if this functionality is included in the implemented slice" and specify inventory functionality | Open |
| R19 | FR-15 Item Support | Remove the "if" statement as this functionality is already included | Critical | Patrick | Alexander | Remove "if this functionality is included in the implemented slice" and specify item functionality | Open |
| R20 | FR-18 External API Isolation | Note should be deleted, it was resolved and was only for the group to see and add which external API is integrated | Major | Sebastian | Alexander | Remove the note, add "(External API chosen is called 11Labs)" | Accepted |
| R21 | NFR-4 Consistent Persistence | This should include EVERYTHING not just what is mentioned | Major | Patrick | Alexander | Change to this "The system shall perserve data consistency" | Accepted |
| R22 | NFR-8 Password Protection | Both User and Admin passwords are saved, this needs more clarity | Major | Mikkel | Alexander | Remove "User" from line 216 | Accepted |
| R23 | NFR-8 Password Protection | We also hash and salt the passwords so this needs to be added | Critical | Sebastian | Alexander | Add "and is also hashed with salt" | Accepted |
| R24 | NFR-10 Input Validation | Client input is not specific enough, we must clarify it | Minor | Patrick | Alexander | Remove "client" and add "input values" | Accepted |
| R25 | NFR-11 Normal Response Time | Quickly is not specifc enough | Major | Mikkel | Alexander | Change to "100 miliseconds" | Accepted |
| R26 | NFR-13 Load Awareness | "required by the course." makes no sense in our context | Suggestion | Patrick | Alexander | Remove "as required by trhe course" | Accepted |
| R27 | NFR-20 Automated API Testing | Missing "Automated External API Testing" | Critical | Sebastian | Alexander | Add sub headers with internal and external API testing, to specify test behaviour | Accepted |
| R28 | NFR-22 Frontend UI Test Tool | "frontend" is pure fluff | Suggestion | Mikkel | Alexander | Remove "frontend" from the line | Accepted |
| R29 | NFR-23 Coverage and Static Analysis | We don't name the tools being used | Major | Mikkel | Alexander | Change to "The project shall included code coverage and the use of SonarCube for static analysis" | Accepted |
| R30 | 6.3 Database | "Software Quality" is not relevant | Suggestion | Patrick | Alexander | Remove "Software Quality" | Accepted |
| R31 | 6.3 Database | Should be specifc for which database is being used | Minor | Mikkel | Alexander | Add "MySQL" as the database used | Accepted |
| R32 | 6.4 Authentication | token-based needs to be more specific about which token type is used | Minor | Patrick | Alexander | Add "JWT" as the token type used | Accepted |
| R33 | US-1 Player Login | The structre of the sentence is incorrect, the meaning is wrong | Major | Sebastian | Alexander | Change to "As a player, I want to log in so that I can access my charactes and see their current game progress" | Accepted |
| R34 | US-4 Make Choice | "actions" does not give an accurate description of what a choice is | Major | Mikkel | Alexander | Change to "As a player, I want to be able to make different choices so that the story changes based on the decisions." | Open |
| R35 | 3.2 Constraints | Automatic failover should be mentioned after "mirrored SQL setup." | Critical | Patrick | Alexander | Add "Which is implemented as an automatic failover." | Accepted |
| R36 | 9. Summary | "Software Quality" is not relevant here | Suggestion | Patrick | Alexander | Remove "Software Quality" | Accepted |
| R37 | 9. Summary | The last sentence is wrong and not needed | Critical | Sebastian | Alexander | Remove the last sentence | Accepted |

## 8. Overall Assessment

### Recommendation
- Mostly accepted with minor changes, few issues still open for rework before approval.

## 10. Follow-Up Actions

| Action ID | Action on | Responsible | Deadline | Status |
|---|---|---|---|---|
| A1 | R15 | Sebastian | 01/06/2026 | Open |
| A2 | R17 | Sebastian | 01/06/2026 | Open |
| A3 | R18 | Sebastian | 01/06/2026 | Open |
| A4 | R19 | Sebastian | 01/06/2026 | Open |
| A5 | R34 | Sebastian | 01/06/2026 | Open |

## 11. Sign-Off

| Name | Role | Sign-Off Status |
|---|---|---|
| Sebastian | Author | Approved |
| Patrick | Moderator | Approved |
| Mikkel | Reviewer | Approved |
| Alexander | Scribe | Approved |