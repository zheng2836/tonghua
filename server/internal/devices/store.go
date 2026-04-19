package devices

import "sync"

type Device struct {
	UserID   string `json:"userId"`
	DeviceID string `json:"deviceId"`
	FCMToken string `json:"fcmToken"`
}

type Store struct {
	mu      sync.RWMutex
	devices map[string]Device
}

func NewStore() *Store {
	return &Store{devices: make(map[string]Device)}
}

func (s *Store) Put(device Device) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.devices[device.UserID] = device
}

func (s *Store) Get(userID string) (Device, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	device, ok := s.devices[userID]
	return device, ok
}

func (s *Store) List() []Device {
	s.mu.RLock()
	defer s.mu.RUnlock()
	result := make([]Device, 0, len(s.devices))
	for _, device := range s.devices {
		result = append(result, device)
	}
	return result
}
