import { useRef, useState } from 'react';

// Retângulo "+" que recebe a foto do animal (input file ou drag-and-drop).
export default function PhotoDropzone({ onPhoto, disabled = false }) {
  const inputRef = useRef(null);
  const [dragOver, setDragOver] = useState(false);

  function handleFile(file) {
    if (!file || disabled) return;
    onPhoto(file);
  }

  return (
    <div
      className={`panel-neo w-full aspect-square max-w-xs mx-auto flex flex-col items-center justify-center gap-3 cursor-pointer select-none
        ${dragOver ? 'bg-forest-100' : 'bg-white'} ${disabled ? 'opacity-50 pointer-events-none' : ''}`}
      role="button"
      tabIndex={0}
      aria-label="Escolher foto para gerar carta"
      onClick={() => inputRef.current?.click()}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          inputRef.current?.click();
        }
      }}
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragOver(false);
        handleFile(e.dataTransfer.files?.[0]);
      }}
    >
      <span className="text-6xl font-black" aria-hidden="true">+</span>
      <p className="font-black uppercase text-center px-4">Tire ou envie uma foto do animal</p>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => handleFile(e.target.files?.[0])}
      />
    </div>
  );
}
