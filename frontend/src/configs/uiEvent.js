// Set VITE_UI_EVENT=none to restore the regular interface across every workspace.
export const uiEvent = (import.meta.env.VITE_UI_EVENT || "christmas").trim().toLowerCase();
export const isChristmas = uiEvent === "christmas";
