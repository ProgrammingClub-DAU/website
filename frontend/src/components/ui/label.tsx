/**
 * Label UI Component
 * Purpose: Form label component for accessibility and consistent styling across auth forms.
 * Auth Connection: Used in /login and /register form fields.
 * Deferred: No backend dependency; purely visual styling component.
 */

import * as React from "react";
import { cn } from "@/lib/utils";

function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        "font-mono text-xs font-semibold tracking-wide text-fg-muted uppercase select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
        className
      )}
      {...props}
    />
  );
}

export { Label };
