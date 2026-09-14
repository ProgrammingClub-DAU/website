"use client";

import React, { ReactNode } from "react";
import { cn } from "@/lib/utils";

export interface Column<T> {
  key: string;
  header: ReactNode;
  render?: (item: T, index: number) => ReactNode;
  className?: string;
  headerClassName?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  isLoading?: boolean;
  emptyMessage?: string;
  className?: string;
  keyExtractor?: (item: T, index: number) => string | number;
}

export function DataTable<T>({
  columns,
  data,
  isLoading = false,
  emptyMessage = "No records found.",
  className,
  keyExtractor,
}: DataTableProps<T>) {
  return (
    <div className={cn("overflow-hidden rounded-panel border border-border bg-surface-1", className)}>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-border bg-surface-2/70 text-xs font-semibold tracking-wider text-fg-muted uppercase">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  scope="col"
                  className={cn("px-4 py-3 text-nowrap", col.headerClassName)}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-border/60">
            {isLoading ? (
              Array.from({ length: 5 }).map((_, rIdx) => (
                <tr key={rIdx} className="animate-pulse">
                  {columns.map((col) => (
                    <td key={col.key} className="px-4 py-3.5">
                      <div className="h-4 w-3/4 rounded bg-surface-2" />
                    </td>
                  ))}
                </tr>
              ))
            ) : data.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-8 text-center text-sm text-fg-muted"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((item, idx) => {
                const key = keyExtractor ? keyExtractor(item, idx) : (item as { id?: string | number })?.id ?? idx;
                return (
                  <tr
                    key={key}
                    className="transition-colors hover:bg-surface-2/40"
                  >
                    {columns.map((col) => {
                      const value = (item as Record<string, unknown>)[col.key];
                      return (
                        <td
                          key={col.key}
                          className={cn("px-4 py-3 text-foreground", col.className)}
                        >
                          {col.render
                            ? col.render(item, idx)
                            : (value as ReactNode) ?? "—"}
                        </td>
                      );
                    })}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default DataTable;
