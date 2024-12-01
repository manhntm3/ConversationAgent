package main

import (
	"bytes"
	"context"
	"flag"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	

	"google.golang.org/grpc"
	// "google.golang.org/grpc/credentials"
	pb "grpc-server/api"
)

var (
	port = flag.Int("port", 50051, "The server port")
)

// server is used to implement 
type server struct {
	pb.UnimplementedChatServiceServer
}

// makeHTTPRequest sends an HTTP POST request to an external API with the provided request body.
func makeHTTPRequest(apiURL string, requestBody map[string]string) (string, error) {
	// Convert the request body to JSON
	jsonData, err := json.Marshal(requestBody)
	if err != nil {
		return "", fmt.Errorf("failed to marshal request body: %v", err)
	}

	// Create an HTTP POST request
	resp, err := http.Post(apiURL, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return "", fmt.Errorf("failed to send HTTP POST request: %v", err)
	}
	defer resp.Body.Close()

	// Read the response body
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read HTTP response body: %v", err)
	}

	// Return the response body as a string
	return string(body), nil
}

// SayHello implements ChatServicesServer
// This method now makes an HTTP request to an external API.
func (s *server) SaySomething(ctx context.Context, in *pb.PromptRequest) (*pb.PromptReply, error) {
	log.Printf("Received: %v", in.GetMessage())

	// External API URL (replace with actual endpoint)
	externalAPIURL := "https://2jn8ttaa1l.execute-api.us-east-1.amazonaws.com/myStage/bedrock"

	// Prepare the request body using data from the gRPC request
	reqBody := map[string]string{
		"prompt": in.GetMessage(),
	}

	// Call the helper function to make the HTTP POST request
	apiResponse, err := makeHTTPRequest(externalAPIURL, reqBody)
	if err != nil {
		return nil, fmt.Errorf("failed to call external API: %v", err)
	}

	// Combine the result of the external API with the greeting message
	// message := fmt.Sprintf("Hello %s! External API Response: %s", in.GetMessage(), apiResponse)

	// return &pb.PromptReply{Message: message}, nil
	return &pb.PromptReply{Message: apiResponse}, nil
}

func main() {
	flag.Parse()

	// creds, err := credentials.NewServerTLSFromFile("RootCA.pem", "RootCA.key")
	// if err != nil {
	// 	log.Fatalf("failed to load TLS credentials: %v", err)
	// }

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	// s := grpc.NewServer(grpc.Creds(creds))
	s := grpc.NewServer()
	pb.RegisterChatServiceServer(s, &server{})
	log.Printf("server listening at %v", lis.Addr())
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
