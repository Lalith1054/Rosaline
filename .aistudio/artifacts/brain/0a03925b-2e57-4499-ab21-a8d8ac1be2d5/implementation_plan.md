# LastDrop — Daily Product Lifespan Tracker

**LastDrop** is a fast, distraction-free utility that tracks how many days your everyday household and personal care products (such as body wash, shampoo, toothpaste, or dish soap) last from the day you open them until the bottle is empty.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The plan below incorporates your preferences confirmed during our initial review. Please check the summary below and click **Proceed** when you are ready to build.

- **Confirmed Decision — Fast Name-Only Logging**: Tapping the `+` button opens a lightweight sheet requiring only the **Product Name** so you can start tracking in seconds.
- **Confirmed Decision — Unified Stacked Home List**: Both currently active items and finished (stopped) products appear together in a single stacked list on the home screen, with visual status badges and quick filter pills (`All`, `In Use`, `Finished`) for easy scanning.
- **Confirmed Decision — Instant On-Device Storage**: All product timers and history are stored locally on your device using **Room SQLite Database**—no account sign-in or network connection required.
- **Confirmed Decision — Rounded Design Language with Nunito Font**: All headings, day counters, body labels, and buttons use the **Nunito** rounded typeface (bundled locally via `res/font/nunito.ttf`), paired with soft pill-shaped buttons and generous rounded card corners (`24.dp`).

---

## 1. Overview & Core Concept

- **What It Does**: Replaces manual calendar tallying with a one-tap product longevity tracker. Add a product by name when you start using it, see its live elapsed day count every time you open the app, and tap **Stop** when it runs out to lock in the total days lasted.
- **Target Audience / Persona**: Everyday consumers who want to know the true lifespan of personal care and household essentials without tedious spreadsheet logging.
- **Key Value**: Instant visibility into how many days each item has been in use (`Day 1`, `14 days`, etc.), side-by-side comparison of active and finished items in a single stacked feed, and one-tap **Restart / New Bottle** or **Delete** actions.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home Screen (Stacked Product List & Summary Banner)**:
   - Displays a top summary header showing the count of **Active Products**, **Finished Products**, and the **Average Lifespan (Days)** of finished items.
   - Underneath, a single stacked list displays every tracked product card.
   - **Active Product Cards** highlight a live pulsing status indicator, the product name, start date, a prominent **Days Used** counter (plus live hours/minutes subtitle for newly started items so you immediately see the timer working), and a tactile **Stop** button.
   - **Finished Product Cards** display a completed badge, start-to-finish date range, the locked-in **Total Days Lasted** badge, and a quick **Track Again** action (to start a fresh bottle with the same name) or **Delete** action.
2. **Adding a New Product (`+` Floating Action Button)**:
   - Tapping the prominent `+` button opens a clean bottom sheet/dialog with an auto-focused **Product Name** input field (plus optional one-tap suggestion chips like *Body Wash*, *Shampoo*, *Toothpaste*, *Face Cleanser* for even faster entry).
   - Tapping **Start Timer** immediately inserts the item at the top of the home screen list with its timer running.
3. **Stopping a Product Timer**:
   - Tapping the **Stop** button on any active product card records the completion timestamp, locks in the total days used, and transitions the card to its **Finished** state within the stacked list.

### Visual Identity & Theme
- **Aesthetic Direction**: *Warm Rounded Apothecary* — friendly, tactile, and approachable with soft rounded geometry (`24.dp` card radii, full-pill buttons and badges) and warm organic surfaces.
- **Color Palette & Mood**:
  - **Background / Surface**: Warm alabaster (`#FAF8F5`) in light mode and deep slate (`#121615`) in dark mode, with elevated rounded card surfaces (`#FFFFFF` / `#1C2220`).
  - **Primary Accent**: Botanical sage-teal (`#1B6B5A` light / `#5CD6B8` dark) for active timers, primary `+` button, and live indicators.
  - **Secondary / Stop Action**: Terracotta coral (`#C85A32` light / `#F08A65` dark) for the pill-shaped **Stop** button to provide clear visual contrast against the active timer state.
  - **Completed State**: Warm stone neutral (`#E8E4DC` / `#28302D`) with crisp typography so finished items remain legible without competing with active timers.
- **Typography & Hierarchy (Nunito Rounded Typeface)**:
  - **Font Family**: **Nunito** (`res/font/nunito.ttf`, bundled locally) used uniformly across the entire app for a cohesive, friendly, rounded look.
  - **Display / Numeric Counters**: *Nunito ExtraBold / Bold* for the large **Days Used** numbers and summary metrics.
  - **Body & Labels**: *Nunito SemiBold / Regular* for product names, start/end date stamps, filter pills, and button labels.
- **Interactive Feedback & Motion**:
  - Smooth card state transitions when a product is stopped.
  - Subtle pulsing dot indicator on active timers.
  - Friendly illustrated empty state on first launch prompting you to tap `+` to log your first product.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Elapsed Days Calculation + Live Sub-Day Counter**
  - *Chosen Approach*: Calculate elapsed days dynamically from the saved `startTimestamp` to the current time (for active items) or `endTimestamp` (for finished items). Count the day you start as **Day 1** (or show exact elapsed days alongside a live `Xh Ym` counter on Day 0–1) so newly added items never look frozen at `0 days` right after creation.
  - *Why*: Gives immediate visual confirmation that the timer is running as soon as you add a product, while still prioritizing the clean number-of-days metric whenever you open the app.
- **Decision 2: Local Persistence via Room Database**
  - *Chosen Approach*: Store all product records in an on-device **Room SQLite database** exposed via Kotlin `Flow`.
  - *Why*: Since timers calculate elapsed duration using persisted start and stop epoch timestamps, the app never drains battery with background services—every time you open the app, it instantaneously computes the exact number of days elapsed from the stored timestamp.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                           │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                     HomeScreen (Scaffold)                      │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │  │
│  │  │  SummaryHeader   │  │   FilterChips    │  │ AddItemFAB   │  │  │
│  │  │ (Active/Avg Days)│  │ (All/Active/Done)│  │  (+ Button)  │  │  │
│  │  └──────────────────┘  └──────────────────┘  └──────┬───────┘  │  │
│  │                                                     │          │  │
│  │  ┌──────────────────────────────────────────────────▼───────┐  │  │
│  │  │               AddProductDialog / Sheet                   │  │  │
│  │  │     (Product Name Input + Quick Chips + Start Button)    │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  │                                                                │  │
│  │  ┌──────────────────────────────────────────────────────────┐  │  │
│  │  │             StackedProductList (LazyColumn)              │  │  │
│  │  │  ┌────────────────────────────────────────────────────┐  │  │  │
│  │  │  │ ProductItemCard                                    │  │  │  │
│  │  │  │ • Title & Start/End Date Metadata                  │  │  │  │
│  │  │  │ • Dynamic Days Counter Badge + Live Elapsed Ticker │  │  │  │
│  │  │  │ • Stop Button (Active) / Restart & Delete (Done)   │  │  │  │
│  │  │  └────────────────────────────────────────────────────┘  │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────┬───────────────────────────────┘  │
└───────────────────────────────────┼──────────────────────────────────┘
                                    │ StateFlow<ProductUiState>
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Presentation Layer (ViewModel)                    │
│  • ProductTrackerViewModel                                           │
│    - Combines Room Flow<List<ProductEntry>> with a periodic ticker   │
│    - Handles addProduct(name), stopProduct(id), restartProduct(item),│
│      deleteProduct(id), and filter selection                         │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │ Repository Pattern
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     Data Layer (Room Persistence)                    │
│  • ProductRepository ──► ProductDao ──► AppDatabase (SQLite)         │
└──────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **`ProductEntry` (Room `@Entity`)**:
  - `id: Long` (auto-generated primary key)
  - `name: String` (product name, e.g., "Body Wash")
  - `startTimeMillis: Long` (epoch timestamp when started)
  - `endTimeMillis: Long?` (`null` while active/running; populated with epoch timestamp when **Stop** is tapped)
  - `isActive: Boolean` (`true` while timer is running, `false` once stopped)

### Interactive Component & State Mapping
- **Add Product (`+` FAB -> Modal)**: Validates that the product name is non-blank, creates a `ProductEntry` with `startTimeMillis = System.currentTimeMillis()`, `endTimeMillis = null`, `isActive = true`, and saves it to Room.
- **Live Elapsed Day & Timer Calculation**: A lightweight coroutine ticker updates the current clock reference every second while the app is open on screen so active items show real-time elapsed duration alongside the calculated **Days Used** (`floor((now - startTimeMillis) / 86_400_000L)` plus a clear day count display).
- **Stop Timer (`Stop` Button)**: Updates the targeted `ProductEntry` in Room with `endTimeMillis = System.currentTimeMillis()` and `isActive = false`, immediately freezing the days counter and updating the card styling in the stacked list.
- **Delete / Track Again**: Users can delete accidental entries (with undo snackbar support) or tap **Restart** on a finished item to log a newly opened replacement with a single tap.
