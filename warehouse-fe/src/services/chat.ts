import { apiFetch } from './api'

export type ChatAskRequest = {
  message: string
}

export type ChatAskResponse = {
  answer: string
  suggestions: string[]
  timestamp: string
}

export const askChatbot = async (message: string): Promise<ChatAskResponse> => {
  return apiFetch<ChatAskResponse>('/chat/ask', {
    method: 'POST',
    body: JSON.stringify({ message } as ChatAskRequest),
  })
}
