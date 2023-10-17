const GRAPHQL_ENDPOINT = "http://localhost:3030/graphql";

let isTestStarted = false
let currentState = "EMPTY"
let shouldUpdate = true

function startTestRecording() {
    document.getElementById("output").innerText = toDate(new Date())
    isTestStarted = true
}

function toDate(timestamp){
    return new Date(timestamp).toLocaleDateString() +" " + new Date(timestamp).toLocaleTimeString()
}

// Function to fetch nodes and update the table

function fetchNodes() {
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

    return fetch(GRAPHQL_ENDPOINT, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ query })
    })
        .then(response => response.json())
        .catch(error => {
            console.error("Error fetching nodes:", error);
        });
}

function createStateToCheck(nodes) {
    return nodes.map(node => node.status.status + ":" + mapNodeName(node.name)).join("\n");
}

function mapNodeName(node) {
    if (node === "worker.07180037-9a1e-4b60-91aa-d83070de3371"){
        return "worker-1"
    } else if (node === "worker.30cbec7e-0976-41b3-b9b1-89f9047cf000") {
        return "worker-2"
    } else if (node === "worker.57eb9fe0-6f46-4f41-84ad-aac60721ee86") {
        return "worker-3"
    } else if (node === "worker.a0c81216-76dd-4f23-8449-feeaadd69d0e") {
        return "worker-4"
    } else {
        return "controller"
    }
}

function appendTableCell(row, content) {
    const cell = document.createElement("td");
    cell.textContent = content;
    row.appendChild(cell);
    return cell;
}

function renderTable(nodes, shouldUpdate, isTestStarted) {
    const tableBody = document.getElementById("nodes-table-body");
    tableBody.innerHTML = ""; // Clear current table rows

    nodes.forEach(node => {
        const row = document.createElement("tr");

        appendTableCell(row, mapNodeName(node.name));
        appendTableCell(row, node.id);
        appendTableCell(row, node.status.status);

        if (shouldUpdate && isTestStarted) {
            const tableBodyTest = document.getElementById("test-transitions-body");
            const rowTest = document.createElement("tr");

            appendTableCell(rowTest, node.status.lastTransition);
            appendTableCell(rowTest, mapNodeName(node.name));
            appendTableCell(rowTest, node.status.status);

            tableBodyTest.appendChild(rowTest);
        }

        appendTableCell(row, toDate(node.status.lastHeartbeat));
        appendTableCell(row, toDate(node.status.lastTransition));
        appendTableCell(row, toDate(node.status.updatedAt));

        const interfacesCell = document.createElement("td");
        const interfaceList = document.createElement("ul");
        node.interfaces.forEach(intf => {
            const element = document.createElement("li");
            element.textContent = `mac address: ${intf.address}\n name: ${intf.name}\nwol: ${intf.wol}`;
            interfaceList.appendChild(element);
        });
        interfacesCell.appendChild(interfaceList);
        row.appendChild(interfacesCell);

        tableBody.appendChild(row);
    });
}

function fetchNodesAndUpdateTable() {
    fetchNodes()
        .then(data => {
            const nodes = data.data.nodes;
            renderTable(nodes, shouldUpdate, isTestStarted); // Assuming shouldUpdate and isTestStarted are available in the outer scope
            shouldUpdate = shouldUpdateChange(createStateToCheck(nodes)); // Assuming shouldUpdateChange is available in the outer scope
        });
}

function shouldUpdateChange(stateToCheck) {

    if (currentState === "EMPTY" && isTestStarted) {
        currentState = stateToCheck
        return true
    }

    if(stateToCheck !== currentState && isTestStarted) {
        currentState = stateToCheck
        return true
    }

    return false
}

// Fetch data from the server
function fetchNodePools() {
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

    return fetch('http://localhost:3030/graphql', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ query }),
    })
        .then(response => response.json())
        .catch(error => {
            console.error("There was an error fetching node pools:", error);
        });
}

// Convert date string to a formatted date
function formatDate(dateString) {
    return new Date(dateString).toLocaleString();
}

// Render the table using fetched data
function renderTableNodePools(nodePools) {
    const tableBody = document.querySelector("#nodePoolsTable tbody");
    tableBody.innerHTML = "";  // Clear the current rows

    nodePools.forEach(nodePool => {
        const row = document.createElement("tr");

        // General properties to display directly
        const properties = ['id', 'name', 'autoScale', 'minNodes', 'maxNodes', 'count', 'createdAt', 'updatedAt'];
        properties.forEach(prop => {
            const cell = document.createElement("td");
            cell.textContent = prop.includes('At') ? formatDate(nodePool[prop]) : nodePool[prop];
            row.appendChild(cell);
        });

        // Handle nodes separately as they're nested
        const nodesCell = document.createElement("td");
        const nodesList = document.createElement("ul");
        nodePool.nodes.forEach(node => {
            const element = document.createElement("li");
            element.textContent = mapNodeName(node.name);
            nodesList.appendChild(element);
        });
        nodesCell.appendChild(nodesList);
        row.appendChild(nodesCell);

        tableBody.appendChild(row);
    });
}

function findNodePoolsAndUpdateTable() {
    fetchNodePools()
        .then(data => {
            renderTableNodePools(data.data.nodePools);
        });
}


function downloadTableAsCSV(tableId, filename) {
    let table = document.getElementById(tableId);
    let rows = Array.from(table.querySelectorAll('tr'));

    // Map each row to a CSV string
    let csvContent = rows.map(row => {
        let cells = Array.from(row.querySelectorAll('td, th'));
        return cells.map(cell => {
            let text = cell.innerText.replace(/"/g, '""'); // escape double quotes
            return `"${text}"`; // enclose each entry in double quotes
        }).join(',');
    }).join('\r\n');

    // Create a Blob object and download the CSV
    let blob = new Blob([csvContent], { type: 'text/csv' });
    let url = window.URL.createObjectURL(blob);

    let a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = filename;

    document.body.appendChild(a);
    a.click();

    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
}

// Fetch nodes and update table every 2 seconds
setInterval(fetchNodesAndUpdateTable, 500);
setInterval(findNodePoolsAndUpdateTable, 4000);
