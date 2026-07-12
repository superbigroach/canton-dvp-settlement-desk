import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Bundled fonts (no external CDN — self-hosted via npm). Inter for the UI,
// JetBrains Mono for every financial figure so blotter columns align.
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@fontsource/inter/700.css';
import '@fontsource/jetbrains-mono/400.css';
import '@fontsource/jetbrains-mono/500.css';
import '@fontsource/jetbrains-mono/700.css';

import './styles.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
