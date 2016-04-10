# MySQL-specific code to create tables for hub.


# ah_nodes contains nodes this node can connect to.
CREATE TABLE ah_nodes (
  uuid               CHAR(36)       NOT NULL,   # UUID identifying a node.
  name               VARCHAR(64)    NOT NULL,   # Node name.
  uri                VARCHAR(256),              # URI at which the node is available.
  PRIMARY KEY (uuid)
);


# ah_hode_details contains details about this node.
CREATE TABLE ah_node_details (
  uuid               CHAR(36)       NOT NULL,   # UUID identifying this auction site.
  type               INTEGER,                   # Note type: site, hub, or root hub.
  name               VARCHAR(64)    NOT NULL,   # Name for this site.
  uri                VARCHAR(256),              # URI at which the server is available to its users.
  up_nodes           TEXT                       # Comma-separated list of upstream node uuids to talk to.
);


# ah_downstream contains downstream nodes that this node talks with.
CREATE TABLE ah_downstream (
  uuid               CHAR(36)     NOT NULL,     # Node UUID.
  type               INTEGER,                   # Node type: site or hub.
  name               VARCHAR(64)  NOT NULL,     # Node name.
  uri                VARCHAR(256) NOT NULL,     # Node URI.
  status             INTEGER,                   # Node status.
  PRIMARY KEY (uuid)
);


# ah_msgs_in contains messages from network received during previous 24 hours.
# This table is used to filter out redundant messages to process each only once.
CREATE TABLE ah_msgs_in (
  uuid               CHAR(36)       NOT NULL,   # UUID iidentifying the message.
  origin             CHAR(36)       NOT NULL,   # UUID of the node the message is from.
  created_timestamp  CHAR(19)       NOT NULL,   # Creation timestamp in format like "2016-04-08 15:00:10".
  message            TEXT           NOT NULL,   # Message.
  status             INTEGER,                   # Status of the message
  PRIMARY KEY (uuid)
);
# TODO: add index by timestamp.


# ah_msgs_out is our outgoing message queue.
# Successfully sent messages are removed from this table.
# Messages not yet sent, or not delivered due to failure stay here for a few retries.
CREATE TABLE ah_msgs_out (
  uuid               CHAR(36)       NOT NULL,   # Random UUID iidentifying the message.
  remote             CHAR(36)       NOT NULL,   # UUID of destination hub.
  created_timestamp  CHAR(19)       NOT NULL,   # Creation timestamp in format like "2016-04-08 15:00:10".
  next_try_timestamp CHAR(19)       NOT NULL,   # timestamp when to try to send it out again.
  message            TEXT           NOT NULL,   # Message.
  status             INTEGER,                   # Status of the message.
  PRIMARY KEY (uuid)
);

