#!/usr/bin/env bash
# Prints a Markdown table of JaCoCo line and branch coverage per module, plus the total, from every
# target/site/jacoco/jacoco.csv under the current directory. CI appends it to the job summary.
set -euo pipefail

mapfile -t reports < <(find . -path '*/target/site/jacoco/jacoco.csv' | sort)
if [ "${#reports[@]}" -eq 0 ]; then
  echo "No JaCoCo reports found."
  exit 0
fi

echo "| Module | Lines | Line coverage | Branches | Branch coverage |"
echo "|---|---:|---:|---:|---:|"
for report in "${reports[@]}"; do
  module=${report#./}
  module=${module%/target/site/jacoco/jacoco.csv}
  # CSV columns: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,
  # BRANCH_COVERED,LINE_MISSED,LINE_COVERED,...
  awk -F, -v module="$module" 'NR > 1 { bm += $6; bc += $7; lm += $8; lc += $9 }
    END {
      printf "| %s | %d/%d | %.1f%% | %d/%d | %.1f%% |\n", module, lc, lc + lm,
        (lc + lm ? 100 * lc / (lc + lm) : 100), bc, bc + bm, (bc + bm ? 100 * bc / (bc + bm) : 100)
    }' "$report"
done
awk -F, 'FNR > 1 { bm += $6; bc += $7; lm += $8; lc += $9 }
  END {
    printf "| **Total** | %d/%d | **%.1f%%** | %d/%d | **%.1f%%** |\n", lc, lc + lm,
      (lc + lm ? 100 * lc / (lc + lm) : 100), bc, bc + bm, (bc + bm ? 100 * bc / (bc + bm) : 100)
  }' "${reports[@]}"
