// Ícone de pegada usado como placeholder quando a carta ainda não tem foto.
export default function PawIcon({ className }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
      <ellipse cx="12" cy="17" rx="6" ry="4.5" />
      <ellipse cx="5.5" cy="9" rx="2.3" ry="3" transform="rotate(-15 5.5 9)" />
      <ellipse cx="10" cy="6" rx="2.3" ry="3" />
      <ellipse cx="14" cy="6" rx="2.3" ry="3" />
      <ellipse cx="18.5" cy="9" rx="2.3" ry="3" transform="rotate(15 18.5 9)" />
    </svg>
  );
}
