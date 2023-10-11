# Allezon service
This project is done as a part of Practical Distributed Systems class at MIM UW, held by RTB House.
Specification of the task is available here: https://github.com/RTBHOUSE/mimuw-lab2023L/tree/master/project

## Overview
This system uses Java, Spring Boot and Spring Data Aerospike for processor and aggregator. It also uses Aerospike database, Kafka for event processing and Haproxy as load balancer.

## Deployment
To deploy:
  1) set up hosts in `hosts` file - it is recommended to use 2 nodes for processors, one of those for haproxy, 5 nodes for aerospike, 2 nodes for kafka and 1 node for aggregator
  2) install ansible
  3) set your user name and password: 
```
export USER=<username>
export PASSWORD=<password>
```
  4) run ansible-playbook:
```
ansible-playbook --extra-vars "ansible_user=$USER ansible_password=$PASSWORD ansible_ssh_extra_args='-o StrictHostKeyChecking=no'" -i hosts main-playbook.yaml
```
  5) wait around 30 seconds for all the services to properly start
  6) entrypoint for service is `<haproxy-node-address>:8088`


