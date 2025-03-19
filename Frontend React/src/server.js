const express = require('express');
const axios = require('axios');
const app = express();

const NGROK_URL = 'https://9c96-103-106-239-104.ap.ngrok.io/api/v1';

app.get('/movies', async (req, res) => {
  try {
    const response = await axios.get(`${NGROK_URL}/movies`);
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching data' });
  }
});

const port = 8080;
app.listen(port, () => console.log(`Server running on http://localhost:${port}`));
