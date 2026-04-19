package push

import (
	"log"
	"sync"
)

type Sender struct {
	mu      sync.RWMutex
	history []IncomingCallPayload
}

type IncomingCallPayload struct {
	Token      string `json:"token"`
	CallID     string `json:"callId"`
	CallerID   string `json:"callerId"`
	CallerName string `json:"callerName"`
}

func NewSender() *Sender {
	return &Sender{}
}

func (s *Sender) SendIncomingCall(payload IncomingCallPayload) error {
	log.Printf("send incoming call push token=%s callId=%s callerId=%s", payload.Token, payload.CallID, payload.CallerID)
	s.mu.Lock()
	s.history = append(s.history, payload)
	s.mu.Unlock()
	return s.sendIncomingCallFCM(payload)
}

func (s *Sender) History() []IncomingCallPayload {
	s.mu.RLock()
	defer s.mu.RUnlock()
	result := make([]IncomingCallPayload, len(s.history))
	copy(result, s.history)
	return result
}
