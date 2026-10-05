import { configureStore } from '@reduxjs/toolkit'
import { contactReducer, uiReducer } from '../pages/app/components/contact-master/apis'
export const store = configureStore({
  reducer: {
    contacts: contactReducer,
    ui: uiReducer,
  },
  middleware: (getDefaultMiddleware) => getDefaultMiddleware({
    serializableCheck: { warnAfter: 128 },
    immutableCheck: { warnAfter: 128 },
  }),
})
export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
export default store
