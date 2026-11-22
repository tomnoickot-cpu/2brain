# Build "2brain" — a minimalist, voice-only iPhone assistant

Build an iOS app called **2brain**. It is a personal assistant you control **only with your voice**. You open it, it greets you, shows what you have today, tomorrow and later this week, and a glowing orb at the bottom is **always listening** for commands. There are no buttons, tabs or menus on the main screen.

The design should feel like the apps from **The Browser Company of New York** (Arc, Dia): calm, warm, paper-like, lots of white space, big elegant serif headings, tiny uppercase labels, soft pastel glows, almost no colour anywhere else.

I'm attaching screenshots of the existing Android version and the app icon. **Match the screenshots as closely as possible.**

---

## 1. Tech requirements

- iOS app (Expo / React Native is fine).
- Continuous speech recognition is the core feature. Use a package that supports live, continuous recognition with partial results (for example `expo-speech-recognition`). If it needs a development build instead of Expo Go, set that up.
- Text-to-speech for replies: `expo-speech`.
- Calendar: `expo-calendar` (read and create events).
- Location + reverse geocoding: `expo-location`.
- Photos: `expo-media-library`.
- Local notifications for timers: `expo-notifications`.
- Fonts: `@expo-google-fonts/instrument-serif` and `@expo-google-fonts/inter`.
- Save small settings (the user's name) with AsyncStorage.
- Support light and dark mode automatically (follow the system setting).

---

## 2. Design system

### Colours

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| paper | `#F7F4EF` | `#131211` | Page background |
| ink | `#1C1B1A` | `#F2EFEA` | Main text |
| muted | `#8C867E` | `#8E887F` | Secondary text |
| faint | `#BDB7AE` | `#55514B` | Past events, status word |
| hairline | ink at 8% | ink at 10% | Card borders |
| card | white at 72% | `#1C1B19` | Day cards |
| glowA | `#FFB38A` apricot | `#FF9466` | Orb + top wash |
| glowB | `#B9A6FF` lavender | `#8E74FF` | Orb + top wash |
| glowC | `#8FD3FF` sky | `#4FB6F5` | Orb |

### Top "wash"
Behind the top ~420pt of every screen, draw two very soft radial gradients: apricot (22% opacity) from the top-left corner and lavender (18%) from the top-right. They fade to transparent. This is the only decoration on the page.

### Typography

| Style | Font | Size / line height | Notes |
| --- | --- | --- | --- |
| Greeting | Instrument Serif | 46 / 50 | letter-spacing −0.5 |
| Mood line | Instrument Serif | 29 / 31 | muted colour |
| Subline (date) | Inter 450 | 15 / 20 | muted |
| Label | Inter 600 | 11 | UPPERCASE, letter-spacing 1.4, muted |
| Body (event titles) | Inter 500 | 16 / 22 | |
| Small (locations) | Inter 400 | 13 / 18 | muted |
| Time | Inter 500 | 13 / 22 | muted |
| Quote (empty states, live transcript) | Instrument Serif *Italic* | 22 / 28 | muted |

Horizontal page padding: **28pt**.

---

## 3. Screens

### 3.1 Welcome (first launch only)
iOS only lets permissions be granted by tapping, so this is the **only** screen with a button.

- The 2brain logo (from the icon) at the top-left, 30pt tall, ink colour.
- "Hey." (greeting style, ink) and on the next line "I'm 2brain." (greeting style, muted).
- Paragraph: "You talk, I handle the rest. To do that I need to hear you, and see a little of your phone."
- Four rows, label on the left (body style, ~118pt wide), reason on the right (muted small):
  - **Microphone**: so you never have to touch the screen
  - **Calendar**: to know your days and add plans
  - **Location**: to tell you where you are, and get you places
  - **Photos**: to pull up pictures when you ask
- The orb (small, 54pt) centred near the bottom.
- A full-width black pill button (ink background, paper text): **"Let's begin"**. It requests microphone + speech recognition, calendar, location (when in use) and photos permissions, one after another.
- If the microphone is denied: grey out the orb, show "2brain is voice-only, so the microphone is the one thing it can't do without." and change the button to "Open settings".

### 3.2 Home (main screen)
A single scrolling column, from top to bottom:

1. **Date line** (subline style): `Sunday, November 22  ·  Brooklyn`. The place is the neighbourhood/city from the user's current location; hide it if location is off.
2. **Greeting** (greeting style) followed by a full stop, chosen by the hour:
   - 5:00–11:59 → "Good morning."
   - 12:00–16:59 → "Good afternoon."
   - 17:00–21:59 → "Good evening."
   - 22:00–4:59 → "Hey."
   - If the user has told the app their name: "Good morning, Alex."
   - Re-check the time every 30 seconds and cross-fade when it changes.
3. **Mood line** (mood style, muted):
   - 5–8 → "Fresh start." · 9–11 → "Here's your day." · 12–16 → "Here's the rest of your day." · 17–21 → "Winding down." · otherwise → "Still up?"
4. 40pt gap.
5. **TODAY card**, then 14pt gap, then **TOMORROW card**. Each card:
   - Rounded 26pt corners, `card` background, 1pt `hairline` border, padding 20 × 18.
   - Header row: label "TODAY" on the left, number of events on the right (faint label style).
   - Each event is a row: time (72pt wide column, "10:30 am", "1 pm", or "All day") · a 6pt dot in the calendar's colour · title (body), with location underneath (small, muted).
   - Events that have already ended: everything in the **faint** colour, grey dot.
   - The next upcoming event today shows "Up next" before its location ("Up next · Studio 4").
   - All-day events come first.
   - Empty Today: "Nothing planned. Enjoy the space." Empty Tomorrow: "Wide open." (quote style.)
6. **LATER** section (only days 2–6 ahead that have events): label "LATER", then for each day a row with "Wed" (body) over "Nov 25" (small, muted) in a 72pt column, and up to 3 event titles on the right, each followed by its time in muted small text ("all day" or "7:30 pm"). If more than 3: "+2 more".
7. Bottom padding of ~230pt so content can scroll above the voice dock.

### 3.3 Voice dock (always visible, pinned to the bottom of every screen)
- Background: vertical gradient from transparent paper to solid paper, so content fades out behind it.
- Centred, from top to bottom:
  - **Text line** (max 3 lines, centred, 36pt side padding):
    - While you're speaking: the live transcript in quotes, quote style, muted: *"what's on tomorrow"*
    - After a reply: the reply text in body style, ink: "Added Coffee with Jo, tomorrow at 8:30 am."
    - Clear the text 9 seconds after the reply.
  - **The orb** (64pt).
  - **Status word** (label style, faint): "LISTENING", "HEARING YOU", "THINKING", "SPEAKING", "ASLEEP · SAY "2BRAIN"", or "MIC UNAVAILABLE".

### 3.4 The orb
A soft, glowing, living sphere. Draw it with layered gradients:
- A large, soft lavender halo around it (about 1.5× the orb's radius, 30% opacity).
- Three blurred colour blobs (apricot, lavender, sky) slowly orbiting the centre, 120° apart.
- A slowly rotating colour core with a soft centre (no hard pie-slice edges), plus a white highlight in the upper-left (65% light / 35% dark) to make it look glassy.

Behaviour:
- **Listening (idle):** breathes slowly (scale 1.0 ↔ 1.04 over 2.6s), blobs rotate once every 9s.
- **Hearing you:** swells with your voice volume (up to +22%), halo grows.
- **Thinking:** spins fast (one turn per 1.4s).
- **Speaking:** halo pulses gently.
- **Asleep / Mic unavailable:** all colours fade to grey over 0.6s.

### 3.5 Overlays (opened by voice, closed by saying "close")
Slide up from the bottom with a fade (0.4s). Same paper background + top wash. The voice dock stays on top.

- Top line (subline, muted): `Say "close" to go back`
- Big title (greeting style): "Yesterday", "This week", "Friday"…
- **Photos overlay:** under the title "15 photos" (muted), then a 3-column grid of square photos with 14pt rounded corners and 6pt gaps, 20pt side padding.
- **Agenda overlay:** one day card (same as Home) per day that has events, titled "Today", "Tomorrow", or "Wednesday, Nov 25".

---

## 4. Voice: how listening works

- When the app is open and in the foreground, it listens **continuously**. When a phrase finishes (about 1.2s of silence), process it, then immediately start listening again. If recognition errors out or times out, quietly restart it after a short pause. iOS recognition sessions are limited to about a minute, so restart them as needed.
- Show partial results live in the dock while the user is speaking.
- **While the app is speaking a reply, stop listening** so it doesn't hear itself, then resume when speech finishes.
- Stop listening when the app goes to the background; resume when it comes back.
- Every command gets a short **spoken** reply (text-to-speech) **and** the same reply shown in the dock.
- Matching should ignore case and punctuation, and treat "p.m."/"pm" and "3:00"/"3" the same.

---

## 5. Commands

| User says (examples) | What happens | Spoken reply (example) |
| --- | --- | --- |
| "What's on today?" / "What's my schedule?" | Read today's events | "4 things today: Run along the river at 7 am, Design review at 10:30 am, Lunch with Maya at 1 pm, and 1 more." |
| "What do I have tomorrow?" / "What's on Friday?" | Open Agenda overlay for that day and read it | "2 things tomorrow: Dentist at 9:15 am, Product sync at 2 pm." |
| "What's on this week?" / "This weekend?" | Agenda overlay for the next 7 days / the weekend | "9 events across 4 days this week." |
| "Add lunch with Sam Friday at 12:30" | Create a 1-hour event | "Added Lunch with Sam, Friday at 12:30 pm." |
| "Remind me to call mom at 6 pm" | Create an event called "Call mom" | "Added Call mom, today at 6 pm." |
| "Add team sync tomorrow at 10 am for 30 minutes" | Event with a custom length | "Added Team sync, tomorrow at 10 am." |
| "Add Ava's birthday on Tuesday" (no time) | All-day event | "Added Ava's birthday, Tuesday." |
| "Show my photos" / "Show photos from yesterday / today / this week / last week / last weekend / this month" | Photos overlay with that date range (newest first, max 60) | "15 photos from yesterday." / "Here are your latest photos." / "No photos from today." |
| "Where am I?" | Current address, also updates the place on Home | "You're at 120 Berry Street, Brooklyn, New York." |
| "Navigate to the airport" / "Directions to Central Park" | Open Apple Maps with directions | "Heading to the airport." |
| "Set a timer for 10 minutes" | In-app countdown + local notification when done (iOS can't set Clock-app timers) | "Timer set for 10 minutes." |
| "Wake me up at 7 am" / "Set an alarm for 7" | Schedule a local notification with sound (explain in the reply that it's a reminder, since iOS doesn't let apps set Clock alarms) | "Reminder set for 7 am." |
| "Call me Alex" / "My name is Alex" | Save the name, use it in the greeting | "Nice to meet you, Alex." |
| "What time is it?" / "What's the date?" | | "It's 3:42 pm." / "It's Sunday, November 22." |
| "Close" / "Go back" / "Done" / "Never mind" | Close any overlay | (no reply) |
| "Go to sleep" / "Stop listening" | Asleep state: still listening, but **only** reacts to the wake phrase | "Going quiet. Say 2brain to wake me." |
| "2brain" / "Hey 2brain" / "Wake up" | Leave Asleep state | "I'm here." |
| "Thanks" / "Thank you" | | "Anytime." |
| "What can you do?" / "Help" | | "Try: what's tomorrow, add lunch with Sam Friday at noon, show photos from yesterday, where am I, set a timer for ten minutes, or go to sleep." |
| Anything else | | "Sorry, I didn't catch that." |

Rules for understanding times and days:
- Days: "today", "tonight", "tomorrow", "the day after tomorrow", weekday names (the next one coming, including today), "next Friday" (skip a week if Friday is today).
- Times: "at 3", "3 pm", "3:30 p.m.", "15:00", "noon", "midnight", "7 in the morning", "8 in the evening".
- A bare "at 1"–"at 7" with no am/pm means the afternoon/evening (1 pm–7 pm).
- A bare number without "at" or am/pm is **not** a time ("dinner with 2" stays in the title).
- If the user gives a time but no day and that time has already passed today, use tomorrow.
- The event title is whatever's left after removing the command words, day and time, with the first letter capitalised.
- Also recognise "Wake me up at 7" as the wake-up reminder, not the "wake up" command.

---

## 6. Other details

- Time format everywhere: lowercase, no ":00" on the hour: "7 am", "10:30 am", "1 pm".
- Use the calendar's own colour for event dots. Read from all visible calendars; write new events to the default calendar.
- The home screen refreshes the calendar and location every time the app comes to the foreground and after adding an event.
- If calendar access is off, show (quote style): `Calendar is off. Say "what can you do" to hear more.`
- If the app needs a permission the user denied, say so out loud: "I need calendar access for that." / "I need location access for that." / "I need access to your photos for that."
- App icon: use the attached icon (black "2" joined to a cloud, on white).
- App name on the home screen: **2brain**.
