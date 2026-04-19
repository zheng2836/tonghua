package push

import "log"

type Sender struct{}

type IncomingCallPayload struct {
    Token      string
    CallID     string
    CallerID   string
    CallerName string
}

func NewSender() *Sender {
    return &Sender{}
}

func (s *Sender) SendIncomingCall(payload IncomingCallPayload) error {
    log.Printf("send incoming call push token=%s callId=%s callerId=%s", payload.Token, payload.CallID, payload.CallerID)
    return nil
}
