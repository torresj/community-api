# Project Architecture: Community Board Meetings Management API

## 1. Overview

This document describes the architecture for a new project (replacing/rebuilding the current one as needed) whose goal is to provide an **authenticated API to manage homeowners' association board meetings ("juntas de vecinos")** for one or more residential communities.

The system must allow users to manage meetings that have already taken place or are scheduled for the future, including:

- Before the meeting takes place: the scheduled date and time, and the agenda items ("puntos") to be voted on.
- After the meeting takes place: the minutes/meeting record data ("acta"), including:
    - The participants in the meeting (identified by **property/unit**, not directly by person).
    - The voting result for each agenda item.
    - When available in the minutes, the individual vote cast by each property for each agenda item.
- The **original signed minutes document (PDF)** must also be stored and made available for consultation.

### Core functional goal

The end goal is to be able to:

1. Represent all the information contained in the minutes ("acta") of a finished meeting.
2. Before the minutes exist, show the scheduled date/time of the meeting and the agenda items to be voted on.

A sample/reference minutes document is available in `specs/docs/` and should be used as the basis for the real data model (fields, structure of agenda items, voting results, attendance, etc.).

---

## 2. User Roles & Permissions

The system has **three levels of users**:

### 2.1 Super Administrator ("superusuario administrador")
- Full access: can create, edit, and delete **everything** in the system.
- Is the **only** role allowed to create new communities.
- Effectively a system-wide administrator, not tied to a single community.

### 2.2 Community Administrator ("administrador de una comunidad")
- Is also a property owner ("propietario") within the community they administer.
- Manages everything related to **their own community**:
    - Add or remove users/owners ("propietarios") within the community they belong to.
    - Create, edit, and delete board meetings ("juntas") and everything related to a meeting (agenda items, minutes, voting results, attached PDF, etc.).
- Scope is limited to the community (or communities) they administer; they are not a super administrator.

### 2.3 User / Owner ("usuario / propietario")
- End users of the system.
- Can view:
    - The communities they belong to.
    - The list of properties they own within each community.
    - The board meetings of the communities they belong to (both scheduled and past).
    - Full meeting minutes data, agenda items, and voting reports.
- **Cannot edit or delete** anything related to the community, meetings, or other users.

---

## 3. Functional Requirements

### 3.1 Communities, properties, and ownership
- A user can belong to one or more communities.
- Within each community, a user can own one or more properties ("propiedades"/viviendas).
- Any user should be able to see:
    - Their communities.
    - Their properties within each community.

### 3.2 Board meetings ("juntas") lifecycle
- A meeting can exist in two main states from a data/visibility standpoint:
    - **Scheduled / upcoming**: only date, time, and the list of agenda items to be voted on are available.
    - **Held / completed**: full minutes ("acta") data is available.

### 3.3 Minutes ("acta") data model
Once a meeting has taken place, the system must be able to represent:
- **Participants in the meeting**, identified by **property** (the unit attending, not necessarily a single named person).
- **Attendance list**, if and when this information is available in the minutes document.
- **Agenda items** ("puntos") discussed and voted on.
- **Voting result per agenda item**:
    - Votes in favor.
    - Votes against.
    - Abstentions.
- **Per-property vote breakdown**, when the minutes document contains that level of detail: what each property voted on each agenda item.
- **Original minutes document (PDF)**: stored and retrievable, linked to its corresponding meeting.

### 3.4 Visibility for end users
Any authenticated user should be able to:
- View their communities and their properties within each community.
- View all the minutes/meeting records, all the agenda items, and the reports for each meeting and each voted item (in favor / against / abstentions).
- View what every owner/property voted on each item, and the attendance list, whenever that information is present in the source minutes.

### 3.5 Search functionality (key feature)
One of the most important functionalities of the system is a **search engine** that allows:
- Searching across **all agenda items of all minutes** in a community (or across communities, depending on the user's permissions).
- Quickly determining whether a specific topic has already been discussed or voted on.
- Identifying **in which meeting and in which specific agenda item** that topic was addressed.

This search capability is considered a core, high-priority feature of the product, not a secondary nice-to-have.

---

## 4. Security

- Security is a **key part of the system**.
- Authentication must be implemented using **JWT (JSON Web Tokens)**.
- Authorization must enforce the three role levels described in Section 2:
    - Super Administrator: unrestricted access.
    - Community Administrator: full management rights, scoped to their own community/communities.
    - User/Owner: read-only access, scoped to the communities and properties they belong to.

---

## 5. Testing Strategy

- Testing is a critical part of this project, with the goal of achieving **very high test coverage**.
- Testing approach should follow a **bottom-up strategy**:
    - Start with **unit tests** for the lowest-level components/layers.
    - Move up to higher-level tests (integration, end-to-end/API-level) only as needed, building on a solid unit test foundation.

---

## 6. PDF Storage Strategy

- The system will be deployed on **Kubernetes**, and a **persistent volume** will be available for storage.
- The original minutes PDFs should be stored on this volume (i.e., as files on disk/persistent storage), **not** as binary content inside the database.
- The database should store only the **file path/reference** to the PDF, rather than the file content itself.
- Each stored PDF must be properly linked to its corresponding meeting record, so it can be retrieved and consulted on demand.

---

## 7. Technology Stack (Reference Only)

The following are placeholders to be confirmed/aligned with the current project; this document does not mandate a specific stack and should be updated once decided:

- **API framework/language**: *to be confirmed* (kept as the current project's stack, or to be decided).
- **Relational database**: *to be confirmed* — assumed to be whatever the current project already uses.
- **File storage**: persistent volume on Kubernetes, as described in Section 6.
- **Authentication**: JWT-based, as described in Section 4.

---

## 8. Reference Material

- `specs/docs/`: contains a sample/reference minutes ("acta") document, to be used as the source of truth for designing the real data model (agenda item structure, voting result format, attendance format, per-property vote detail, etc.).
