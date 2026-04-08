Claude Code's source code leaked last week.
512,000 lines of TypeScript.

Most people focused on the drama.
I focused on the memory architecture.

Here's how Claude Code actually remembers things across sessions — and why it's a masterclass in agent design:

𝗧𝗵𝗲 𝟯-𝗟𝗮𝘆𝗲𝗿 𝗠𝗲𝗺𝗼𝗿𝘆 𝗔𝗿𝗰𝗵𝗶𝘁𝗲𝗰𝘁𝘂𝗿𝗲:

𝗟𝗮𝘆𝗲𝗿 𝟭 — 𝗠𝗘𝗠𝗢𝗥𝗬. 𝗺𝗱 (𝗔𝗹𝘄𝗮𝘆𝘀 𝗟𝗼𝗮𝗱𝗲𝗱) A lightweight index file. Not storage — pointers. Each line is under 150 characters. First 200 lines get injected into context at every session start. It points to topic files. It never holds the actual knowledge.
Think of it as a table of contents, not the book.

𝗟𝗮𝘆𝗲𝗿 𝟮 — 𝗧𝗼𝗽𝗶𝗰 𝗙𝗶𝗹𝗲𝘀 (𝗢𝗻-𝗗𝗲𝗺𝗮𝗻𝗱) Detailed knowledge spread across separate markdown files. Architecture decisions. Naming conventions. Test commands. Loaded only when MEMORY. md says they're relevant.
Not everything gets loaded. Only what's needed right now.

𝗟𝗮𝘆𝗲𝗿 𝟯 — 𝗥𝗮𝘄 𝗧𝗿𝗮𝗻𝘀𝗰𝗿𝗶𝗽𝘁𝘀 (𝗚𝗿𝗲𝗽-𝗕𝗮𝘀𝗲𝗱 𝗦𝗲𝗮𝗿𝗰𝗵) Past session transcripts are never fully reloaded. They're searched using grep for specific identifiers. Fast. Deterministic. No embeddings. No vector DB.
Just plain text search when the first two layers aren't enough.

But here's the part that blew my mind:
𝗦𝗸𝗲𝗽𝘁𝗶𝗰𝗮𝗹 𝗠𝗲𝗺𝗼𝗿𝘆.
The agent treats its own memory as a hint, not a fact.
Memory says a function exists? 
→ Verify against the codebase first. Memory says a file is at this path? 
→ Check before using it.

And one more design principle hidden in the code:
If something can be re-derived from source code — it doesn't get stored.
Code patterns, conventions, architecture? Excluded from memory saves entirely.

Because if it can be looked up, it shouldn't be remembered.

𝗪𝗵𝘆 𝘁𝗵𝗶𝘀 𝗺𝗮𝘁𝘁𝗲𝗿𝘀 𝗯𝗲𝘆𝗼𝗻𝗱 𝗖𝗹𝗮𝘂𝗱𝗲 𝗖𝗼𝗱𝗲:
This 3-layer pattern is model-agnostic.
Any team building AI agents can steal this:
→ Keep your always-loaded context tiny 
→ Reference everything else via pointers 
→ Never persist what can be looked up 
→ Treat memory as a hint, not truth

The future of AI agents isn't about how much they remember.
It's about how well they forget.

What memory patterns are you using in your agent builds?