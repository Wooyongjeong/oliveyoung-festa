# Repository workflow

## Required reading

Before changing code, read:

1. `README.md`
2. `docs/README.md`
3. `docs/IMPLEMENTATION_PLAN.md`
4. The active file under `docs/phases/`

Use `docs/phases/README.md` to identify the current phase. Do not start a later phase while an earlier phase has incomplete required checks unless the user explicitly changes the order.

## Phase documentation rule

Every implementation change must belong to a phase document under `docs/phases/`.

Before implementation:

- Confirm the phase objective and scope.
- Add or refine checklist items when the plan has become more specific.
- Keep unrelated future-phase work out of the change.

After implementation:

- Check only items that are actually complete.
- Update `구현 내용` with the concrete files, APIs, schema, and behavior added.
- Update `검증 결과` with the exact commands or scenarios that passed.
- Record remaining work or known limitations; never mark a phase complete with required work remaining.
- Update the status table in `docs/phases/README.md`.

When all required checks pass, change the phase status to `완료`. Documentation updates are part of the phase completion criteria, not optional follow-up work.

## General constraints

- Preserve the MVP policies in `docs/IMPLEMENTATION_PLAN.md`.
- Prefer the smallest implementation that satisfies the active phase.
- PostgreSQL remains the source of truth for domain state.
- Run tests proportional to the change and record them in the phase document.
- Keep `README.md` at the repository root; place other project documentation under `docs/`.
