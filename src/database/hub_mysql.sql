# MySQL-specific code to create tables for hub.


# ah_node_details contains details about this node.
CREATE TABLE ah_node_details (
  uuid               CHAR(36)       NOT NULL,   # UUID identifying this node.
  type               INTEGER,                   # Note type: site, hub, or root hub.
  name               VARCHAR(64)    NOT NULL,   # Name for this site.
  uri                VARCHAR(256),              # URI at which the server is available to its users.
  up_nodes           TEXT                       # Comma-separated list of upstream node uuids to talk to.
);


# ah_nodes contains nodes that this node talks with.
# They are upstream nodes, downstream nodes, and locally attached servers.
CREATE TABLE ah_nodes (
  uuid               CHAR(36)     NOT NULL,     # Node UUID.
  type               INTEGER,                   # Node type: localhost (NULL), downstream (0), or upstream(1).
  name               VARCHAR(64)  NOT NULL,     # Node name.
  uri                VARCHAR(256) NOT NULL,     # Node URI.
  status             INTEGER,                   # Node status.
  PRIMARY KEY (uuid)
);

# Insert default upstream nodes.
INSERT INTO ah_nodes values('cf9ab125-1968-4149-89f6-47100f3b92bb', 1, 'test node 1', 'localhost:8090', 1);
INSERT INTO ah_nodes values('008d8fac-3619-4466-8d46-ff9caf22a04b', 1, 'test node 2', 'localhost:8091', 1);
# Inserat default localhost.
INSERT INTO ah_nodes values('51431704-fe3f-4d93-a0ad-c853d0a39e47', NULL, 'Local Auction Server', 'localhost:8099', 1);


# ah_inbound contains messages from network received during previous 24 hours.
# This table is used to filter out redundant messages to process each only once.
CREATE TABLE ah_inbound (
  uuid               CHAR(36)       NOT NULL,   # UUID iidentifying the message.
  origin             CHAR(36),                  # UUID of the node the message is from.
  created_timestamp  CHAR(19),                  # Creation timestamp in format like "2016-04-08 15:00:10".
  message            TEXT,                      # Message.
  type               INTEGER,                   # Message type.
  status             INTEGER,                   # Status of the message
  PRIMARY KEY (uuid)
);
# TODO: add index by timestamp.


# ah_outbound is our outgoing message queue.
# Successfully sent messages are removed from this table.
# Messages not yet sent, or not delivered due to failure stay here for a few retries.
CREATE TABLE ah_outbound (
  uuid               CHAR(36)       NOT NULL,   # Random UUID iidentifying the message.
  remote             CHAR(36)       NOT NULL,   # UUID of destination hub.
  created_timestamp  CHAR(19),                  # Creation timestamp in format like "2016-04-08 15:00:10".
  next_try_timestamp CHAR(19),                  # timestamp when to try to send it out again.
  message            TEXT,                      # Message.
  type               INTEGER,                   # Message type.
  attempts           INTEGER,                   # Number of unsuccessful delivery attempts.
  status             INTEGER                    # Status of the message.
);

