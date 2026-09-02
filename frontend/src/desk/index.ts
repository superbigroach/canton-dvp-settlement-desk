// The product client the pages import. VITE_API_MOCK=1 swaps in the in-memory stand-in.
import { mockClient } from './mock';
import { realClient, type DeskClient } from './client';

export const MOCK = import.meta.env.VITE_API_MOCK === '1';
export const desk: DeskClient = MOCK ? mockClient : realClient;
export * from './types';
