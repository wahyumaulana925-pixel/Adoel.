import { useState } from "react";
import { formatYard } from "../domain/format";
import type { Estimasi, MesinData } from "../domain/types";

const KETERANGAN_CODES = ["HB", "P.LP", "P.SN", "P.OH", "P.EL", "P.Sel"];

type Step = "SETUP" | "CHOOSE" | "NORMAL" | "KETERANGAN";

/** Terpandu (guided) DOFFING — langkah pertama menawarkan pilihan besar ("Doffing normal" /
 * "Ada keterangan") sebagai pengganti teks bebas. Setiap jalur berakhir membangun string
 * command yang identik dengan yang dulu dikirim konsol Teks. Port 1:1 dari
 * GuidedDoffingSheet.kt (aplikasi Android). Update bacaan counter D408 sepenuhnya lewat
 * tombol Estimasi (yang sudah otomatis minta field "Bacaan jam counter" untuk tipe D408) —
 * bukan lewat menu Doffing ini, supaya tidak ada dua jalur berbeda menuju hasil yang sama. */
export function GuidedDoffingSheet({
  mcNo,
  mesin,
  estimasi,
  onDismiss,
  onSubmitDoffing,
  onQuickUpdate,
}: {
  mcNo: string;
  mesin: MesinData | null;
  estimasi: Estimasi | null;
  onDismiss: () => void;
  onSubmitDoffing: (value: string) => void;
  onQuickUpdate: (corak: string, targetYard: number | null) => void;
}) {
  const needsSetup = !mesin || mesin.corak.trim() === "" || mesin.corak.trim() === "-";
  const [step, setStep] = useState<Step>(needsSetup ? "SETUP" : "CHOOSE");
  const standardYard = estimasi?.yardOverride ?? mesin?.targetYard ?? null;

  return (
    <div className="dialog-backdrop" onClick={onDismiss}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16 }}>Catat Doffing — Mc {mcNo}</div>

        {step === "SETUP" && (
          <SetupStep
            onSave={(corak, targetYard) => {
              onQuickUpdate(corak, targetYard);
              setStep("CHOOSE");
            }}
            onCancel={onDismiss}
          />
        )}
        {step === "CHOOSE" && (
          <ChooseStep
            onPickNormal={() => setStep("NORMAL")}
            onPickKeterangan={() => setStep("KETERANGAN")}
            onCancel={onDismiss}
          />
        )}
        {step === "NORMAL" && (
          <NormalYardStep
            standardYard={standardYard}
            onBack={() => setStep("CHOOSE")}
            onConfirm={(yard) => onSubmitDoffing(`${mcNo} ${yard}`)}
          />
        )}
        {step === "KETERANGAN" && (
          <KeteranganStep
            standardYard={standardYard}
            onBack={() => setStep("CHOOSE")}
            onConfirm={(cmd) => onSubmitDoffing(`${mcNo} ${cmd}`)}
          />
        )}
      </div>
    </div>
  );
}

function SetupStep({ onSave, onCancel }: { onSave: (corak: string, targetYard: number | null) => void; onCancel: () => void }) {
  const [corakInput, setCorakInput] = useState("");
  const [targetYardInput, setTargetYardInput] = useState("");

  return (
    <>
      <div className="field-label">Masukkan Corak Baru</div>
      <input
        className="field-input"
        placeholder="cth: 34758"
        inputMode="numeric"
        value={corakInput}
        onChange={(e) => setCorakInput(e.target.value.toUpperCase())}
      />
      <div style={{ height: 12 }} />
      <div className="field-label">Target Yard (Opsional)</div>
      <input
        className="field-input"
        placeholder="cth: 303"
        inputMode="decimal"
        value={targetYardInput}
        onChange={(e) => setTargetYardInput(e.target.value)}
      />
      <div className="actions" style={{ marginTop: 20 }}>
        <button className="cancel" onClick={onCancel}>
          Batal
        </button>
        <button
          className="confirm"
          style={{ background: "var(--cyan-600)" }}
          disabled={corakInput.trim() === ""}
          onClick={() => {
            if (corakInput.trim() === "") return;
            const yard = targetYardInput.trim() === "" ? null : parseFloat(targetYardInput.trim().replace(",", "."));
            onSave(corakInput.trim(), yard != null && !Number.isNaN(yard) ? yard : null);
          }}
        >
          Simpan &amp; Lanjut
        </button>
      </div>
    </>
  );
}

function ChooseStep({
  onPickNormal,
  onPickKeterangan,
  onCancel,
}: {
  onPickNormal: () => void;
  onPickKeterangan: () => void;
  onCancel: () => void;
}) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      <BigChoiceButton label="Doffing normal" subtitle="Selesai sesuai target yard" onClick={onPickNormal} />
      <BigChoiceButton
        label="Ada keterangan"
        subtitle="HB, P.LP, P.SN, P.OH, P.EL, P.Sel, atau lainnya"
        onClick={onPickKeterangan}
      />
      <button className="btn" style={{ marginTop: 6 }} onClick={onCancel}>
        Batal
      </button>
    </div>
  );
}

function BigChoiceButton({
  label,
  subtitle,
  onClick,
  accent = "var(--cyan-600)",
}: {
  label: string;
  subtitle: string;
  onClick: () => void;
  accent?: string;
}) {
  return (
    <button className="big-choice-btn" onClick={onClick}>
      <div className="label" style={{ color: accent }}>
        {label}
      </div>
      <div className="subtitle">{subtitle}</div>
    </button>
  );
}

function NormalYardStep({
  standardYard,
  onBack,
  onConfirm,
}: {
  standardYard: number | null;
  onBack: () => void;
  onConfirm: (yard: string) => void;
}) {
  const [yardInput, setYardInput] = useState(standardYard != null ? formatYard(standardYard) : "");

  return (
    <>
      <YardDeltaField standardYard={standardYard} yardInput={yardInput} onYardInputChange={setYardInput} />
      <div className="actions" style={{ marginTop: 20 }}>
        <button className="cancel" onClick={onBack}>
          Kembali
        </button>
        <button
          className="confirm"
          style={{ background: "var(--cyan-600)" }}
          disabled={yardInput.trim() === ""}
          onClick={() => yardInput.trim() !== "" && onConfirm(yardInput.trim())}
        >
          Simpan
        </button>
      </div>
    </>
  );
}

function YardDeltaField({
  standardYard,
  yardInput,
  onYardInputChange,
}: {
  standardYard: number | null;
  yardInput: string;
  onYardInputChange: (v: string) => void;
}) {
  function step(delta: number) {
    const current = parseFloat(yardInput.trim().replace(",", ".")) || standardYard || 0;
    onYardInputChange(formatYard(current + delta));
  }

  return (
    <>
      <div className="field-label">Yard aktual</div>
      <input
        className="field-input"
        placeholder={standardYard != null ? `Standar: ${formatYard(standardYard)}y` : undefined}
        inputMode="decimal"
        value={yardInput}
        onChange={(e) => onYardInputChange(e.target.value)}
      />
      <div className="yard-step-row">
        {[-5, -1, 1, 5].map((delta) => (
          <button key={delta} className="yard-step-btn" onClick={() => step(delta)}>
            {delta > 0 ? `+${delta}` : delta}
          </button>
        ))}
      </div>
    </>
  );
}

function KeteranganStep({
  standardYard,
  onBack,
  onConfirm,
}: {
  standardYard: number | null;
  onBack: () => void;
  onConfirm: (cmd: string) => void;
}) {
  const [ket, setKet] = useState("");
  const [yardInput, setYardInput] = useState("");

  function toggleDelta() {
    setYardInput((y) => (y.startsWith("+") ? y.slice(1) : "+" + y.replace(/^-/, "")));
  }

  return (
    <>
      <div className="field-label">Keterangan</div>
      <div className="chip-row-wrap">
        {KETERANGAN_CODES.map((code) => (
          <button key={code} className={`chip-btn${ket === code ? " active" : ""}`} onClick={() => setKet(code)}>
            {code}
          </button>
        ))}
      </div>
      <div style={{ height: 8 }} />
      <input
        className="field-input"
        placeholder="Ketik keterangan lain jika tidak ada di atas"
        value={ket}
        onChange={(e) => setKet(e.target.value.toUpperCase())}
      />

      <div style={{ height: 14 }} />
      <div className="field-label">Yard aktual (opsional)</div>
      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <input
          className="field-input"
          style={{ flex: 1 }}
          placeholder={standardYard != null ? `Standar: ${formatYard(standardYard)}y` : "cth: 70"}
          inputMode="decimal"
          value={yardInput}
          onChange={(e) => setYardInput(e.target.value)}
        />
        <button className={`delta-toggle-btn${yardInput.startsWith("+") ? " active" : ""}`} onClick={toggleDelta}>
          +
        </button>
      </div>

      <div className="actions" style={{ marginTop: 20 }}>
        <button className="cancel" onClick={onBack}>
          Kembali
        </button>
        <button
          className="confirm"
          style={{ background: "var(--cyan-600)" }}
          disabled={ket.trim() === ""}
          onClick={() => {
            const cmd = [ket.trim(), yardInput.trim()].filter((s) => s.length > 0).join(" ");
            if (cmd.trim() !== "") onConfirm(cmd);
          }}
        >
          Simpan
        </button>
      </div>
    </>
  );
}
