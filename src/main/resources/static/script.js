const GRAPHQL_ENDPOINT = "http://localhost:3030/graphql";

function toDate(timestamp){
    return new Date(timestamp).toLocaleDateString() +" " + new Date(timestamp).toLocaleTimeString()
}

// Function to fetch nodes and update the table
function fetchNodesAndUpdateTable() {
    const query = `{
        nodes {
            name
            id
            status {
                status
                reason
                message
                lastHeartbeat
                lastTransition
                updatedAt
            }
            interfaces {
                id
                name
                address
                wol
                createdAt
                updatedAt
                speed
            }
        }
    }`;

    // Use the Fetch API to send the GraphQL query
    fetch(GRAPHQL_ENDPOINT, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ query })
    })
    .then(response => response.json())
    .then(data => {
        const nodes = data.data.nodes;
        const tableBody = document.getElementById("nodes-table-body");
        tableBody.innerHTML = ""; // Clear current table rows

        nodes.forEach(node => {
            
            let row = document.createElement("tr")
            
            let nodeNameCell = document.createElement("td")
            nodeNameCell.textContent = node.name
            row.appendChild(nodeNameCell)

            let nodeIdCell = document.createElement("td")
            nodeIdCell.textContent = node.id
            row.appendChild(nodeIdCell)
            
            let nodeStatus = document.createElement("td")
            nodeStatus.textContent = node.status.status
            row.appendChild(nodeStatus)
            
            let nodeHeartbeat = document.createElement("td")
            nodeHeartbeat.textContent = toDate(node.status.lastHeartbeat)
            row.appendChild(nodeHeartbeat)
            
            let nodeTransition = document.createElement("td")
            nodeTransition.textContent = toDate(node.status.lastTransition)
            row.appendChild(nodeTransition)
           
            let nodeUpdateAt = document.createElement("td")
            nodeUpdateAt.textContent = toDate(node.status.updatedAt)
            row.appendChild(nodeUpdateAt)

            let interfacesCell = document.createElement("td");
            let interfaceList = document.createElement("ul")

            // Here, we'll simply list the names of the interfaces for brevity.
            // You can adjust this to display more detailed information if needed.
            node.interfaces.forEach(intf => {
                let element = document.createElement("li")
                element.textContent = "mac address: " + intf.address + "\n name: " + intf.name + "\nwol: " + intf.wol
                interfaceList.appendChild(element)
            });
            interfacesCell.appendChild(interfaceList)
            row.appendChild(interfacesCell);


            tableBody.appendChild(row);
            // const row = `<tr>
            //     <td>${node.name}</td>
            //     <td>${node.id}</td>
            //     <td>${node.status.status}</td>
            //     <td>${node.status.reason || "-"}</td>
            //     <td>${node.status.message || "-"}</td>
            //     <td>${node.status.lastHeartbeat || "-"}</td>
            //     <td>${toDate(node.status.lastTransition)}</td>
            //     <td>${toDate(node.status.updatedAt)}</td>
            // </tr>`;
        });
    })
    .catch(error => {
        console.error("Error fetching nodes:", error);
    });
}

function findNodePoolsAndUpdateTable() {
    const query = `
    {
        nodePools {
            id
            name
            autoScale
            minNodes
            maxNodes
            count
            createdAt
            updatedAt
            nodes {
                id
                name
            }
        }
    }
    `;

    fetch('http://localhost:3030/graphql', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            query: query
        }),
    })
    .then(response => response.json())
    .then(data => {
        let tableBody = document.querySelector("#nodePoolsTable tbody");
        tableBody.innerHTML = "";  // Clear the current rows

        data.data.nodePools.forEach(nodePool => {
            let row = document.createElement("tr");

            // Create cells for each property
            let idCell = document.createElement("td");
            idCell.textContent = nodePool.id;
            row.appendChild(idCell);

            let nameCell = document.createElement("td");
            nameCell.textContent = nodePool.name;
            row.appendChild(nameCell);

            let autoScaleCell = document.createElement("td");
            autoScaleCell.textContent = nodePool.autoScale;
            row.appendChild(autoScaleCell);

            let minNodesCell = document.createElement("td");
            minNodesCell.textContent = nodePool.minNodes;
            row.appendChild(minNodesCell);

            let maxNodesCell = document.createElement("td");
            maxNodesCell.textContent = nodePool.maxNodes;
            row.appendChild(maxNodesCell);

            let countCell = document.createElement("td");
            countCell.textContent = nodePool.count;
            row.appendChild(countCell);

            let createdAtCell = document.createElement("td");
            createdAtCell.textContent = new Date(nodePool.createdAt).toLocaleString();
            row.appendChild(createdAtCell);

            let updatedAtCell = document.createElement("td");
            updatedAtCell.textContent = new Date(nodePool.updatedAt).toLocaleString();
            row.appendChild(updatedAtCell);

            let nodesCell = document.createElement("td");
            let nodesList = document.createElement("ul")
            nodesCell.appendChild(nodesList)
            nodePool.nodes.forEach(node => {
                let element = document.createElement("li")
                element.textContent = node.name
                nodesList.appendChild(element)
            })
            row.appendChild(nodesCell);

            tableBody.appendChild(row);
        });
    })
    .catch(error => {
        console.error("There was an error fetching node pools:", error);
    });
}

// Fetch nodes and update table every 2 seconds
setInterval(fetchNodesAndUpdateTable, 2000);
setInterval(findNodePoolsAndUpdateTable, 2000);
