use crate::errors::LibError;
use core::fmt::Debug;
use std::io::Write;
use std::process::{Child, ChildStdin, Command, Stdio};

pub struct Encoder {
    ffmpeg: Child,
    stdin: Option<ChildStdin>,
}

impl Debug for Encoder {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Encoder").finish()
    }
}

impl Encoder {
    pub fn new(output: &str, width: u32, height: u32) -> Result<Self, LibError> {
        let mut ffmpeg = Command::new("ffmpeg")
            .args([
                "-f",
                "rawvideo",
                "-pixel_format",
                "rgb24",
                "-video_size",
                format!("{width}x{height}").as_str(),
                "-i",
                "-",
                "-c:v",
                "libx264",
                "-preset",
                "medium",
                "-crf",
                "23",
                "-pix_fmt",
                "yuv420p",
                "-vf",
                "scale=trunc(iw/2)*2:trunc(ih/2)*2",
                output,
            ])
            .stdin(Stdio::piped())
            .spawn()
            .map_err(|_e| LibError::from_str("Failed to spawn ffmpeg"))?;

        let stdin = ffmpeg
            .stdin
            .take()
            .ok_or_else(|| LibError::from_str("Failed to get stdin"))?;

        Ok(Self {
            ffmpeg,
            stdin: Some(stdin),
        })
    }

    pub fn write(&mut self, data: &[u8]) -> Result<(), LibError> {
        let stdin = self
            .stdin
            .as_mut()
            .ok_or_else(|| LibError::from_str("Failed to get stdin"))?;
        stdin
            .write_all(data)
            .map_err(|_e| LibError::from_str("Failed to write to ffmpeg"))?;
        Ok(())
    }

    pub fn finish(&mut self) -> Result<(), LibError> {
        // Decompose self to drop ffmpeg and stdin
        let Self {
            ref mut ffmpeg,
            stdin,
        } = self;
        let stdin = stdin
            .take()
            .ok_or_else(|| LibError::from_str("Failed to get stdin"))?;
        drop(stdin);
        ffmpeg
            .wait()
            .map_err(|_e| LibError::from_str("Failed to wait for ffmpeg"))?;

        Ok(())
    }
}
