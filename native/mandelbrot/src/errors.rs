use std::cell::RefCell;
thread_local! {
    static LAST_ERROR: RefCell<Option<String>> = RefCell::new(None);
}

#[derive(Debug)]
pub struct LibError {
    message: String,
}

impl LibError {
    pub fn new(message: String) -> Self {
        Self { message }
    }

    pub fn from_str(message: &str) -> Self {
        Self::new(message.to_string())
    }

    pub fn message(&self) -> &str {
        self.message.as_str()
    }

    pub fn set_last_error(&self) {
        LAST_ERROR.with(|last_error| {
            *last_error.borrow_mut() = Some(self.message.clone());
        });
    }
}

pub fn clear_last_error() {
    LAST_ERROR.with(|last_error| {
        *last_error.borrow_mut() = None;
    });
}

pub fn get_last_error() -> Option<String> {
    LAST_ERROR.with(|last_error| last_error.borrow().clone())
}

pub fn handle_errors<T>(
    func: impl Fn() -> Result<T, LibError> + std::panic::UnwindSafe,
    error_value: fn() -> T,
) -> T {
    clear_last_error();
    let result = std::panic::catch_unwind(move || func());
    match result {
        Ok(result) => match result {
            Ok(value) => value,
            Err(error) => {
                error.set_last_error();
                error_value()
            }
        },
        Err(_) => {
            let error = LibError::from_str("Panic");
            error.set_last_error();
            error_value()
        }
    }
}
