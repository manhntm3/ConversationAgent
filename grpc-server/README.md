
### Go gRPC server

Receive gRPC call, forward call to API Gateway
Step to run gRPC server:
```
go run server/main.go
```

### Deploy to EC2

- Create an EC2 instance that suitable for use (ideally with more than 4GB RAM and 20GB memory)
- Set up Security Group for EC2 instance, allow HTTP over TCP at port 8000 for REST server and port 22 for ssh.
- SSH into the EC2 instance, clone this repo
- Install java, scala, go with the specified version
- Run RESTful server and gRPC server 

